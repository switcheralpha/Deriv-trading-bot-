#!/usr/bin/env python3
"""
Deriv XAUUSD (Gold) Mobile Trading Bot
Single-File Cross-Platform Kivy Application (main.py)
Connects to Deriv Broker WebSocket API for real-time tick streaming and market order execution.
"""

import json
import threading
import time
from collections import deque
from datetime import datetime

# Kivy GUI Components
from kivy.app import App
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.graphics import Color, RoundedRectangle, Line
from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.scrollview import ScrollView
from kivy.uix.label import Label
from kivy.uix.textinput import TextInput
from kivy.uix.button import Button

# WebSocket client (websocket-client library: pip install websocket-client)
try:
    import websocket
except ImportError:
    websocket = None

# Set default window size for mobile emulation on desktop
Window.clearcolor = (0.05, 0.06, 0.09, 1.0)  # Sleek dark background
Window.size = (400, 750)


class DarkCard(BoxLayout):
    """Custom styled container with rounded dark card background and subtle border."""
    def __init__(self, bg_color=(0.09, 0.11, 0.16, 1.0), border_color=(0.18, 0.22, 0.30, 0.7), radius=dp(10), **kwargs):
        super().__init__(**kwargs)
        self.bg_color = bg_color
        self.border_color = border_color
        self.radius = radius
        with self.canvas.before:
            self.col_bg = Color(*self.bg_color)
            self.rect = RoundedRectangle(pos=self.pos, size=self.size, radius=[self.radius])
            self.col_border = Color(*self.border_color)
            self.border_line = Line(rounded_rectangle=(self.x, self.y, self.width, self.height, self.radius), width=1.1)
        self.bind(pos=self.update_canvas, size=self.update_canvas)

    def update_canvas(self, *args):
        self.rect.pos = self.pos
        self.rect.size = self.size
        self.border_line.rounded_rectangle = (self.x, self.y, self.width, self.height, self.radius)


class DerivTradingBotApp(App):
    """Deriv Gold (XAUUSD) Algorithmic & Manual Trading Engine App."""

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.title = "Deriv Gold Bot (XAUUSD)"
        
        # State variables
        self.ws = None
        self.ws_thread = None
        self.is_running = False
        self.is_authenticated = False
        self.is_bot_active = False

        # Market & Trading Telemetry
        self.symbol = "frxXAUUSD"
        self.current_price = 0.0
        self.price_history = deque(maxlen=60)
        self.fast_period = 5
        self.slow_period = 15
        self.last_signal = None
        self.last_trade_time = 0
        self.min_trade_interval_sec = 15  # Cooldown between automated trades

    def build(self):
        # Root layout
        root = BoxLayout(orientation='vertical', padding=dp(12), spacing=dp(10))

        # 1. Header & Title Banner
        header = DarkCard(
            orientation='vertical',
            size_hint_y=None,
            height=dp(70),
            padding=[dp(12), dp(8)],
            bg_color=(0.08, 0.10, 0.15, 1.0),
            border_color=(0.85, 0.65, 0.13, 0.4)
        )
        title_label = Label(
            text="[b][color=F59E0B]DERIV[/color] GOLD TRADER[/b]",
            markup=True,
            font_size='18sp',
            size_hint_y=0.55,
            halign='center'
        )
        sub_title = Label(
            text="XAUUSD Real-Time WebSocket Bot & Override",
            font_size='11sp',
            color=(0.6, 0.65, 0.75, 1.0),
            size_hint_y=0.45,
            halign='center'
        )
        header.add_widget(title_label)
        header.add_widget(sub_title)
        root.add_widget(header)

        # 2. Status & Live Telemetry Card
        telemetry_card = DarkCard(
            orientation='vertical',
            size_hint_y=None,
            height=dp(105),
            padding=dp(10),
            spacing=dp(4),
            bg_color=(0.07, 0.09, 0.14, 1.0)
        )
        self.status_label = Label(
            text="[b]STATUS:[/b] [color=EF4444]Disconnected[/color]",
            markup=True,
            font_size='16sp',
            size_hint_y=None,
            height=dp(28),
            halign='center'
        )
        self.price_label = Label(
            text="[color=9CA3AF]XAUUSD:[/color] [b][color=FBBF24]---.--[/color][/b] USD",
            markup=True,
            font_size='20sp',
            size_hint_y=None,
            height=dp(34),
            halign='center'
        )
        self.sma_label = Label(
            text="Fast SMA(5): -- | Slow SMA(15): -- | Signal: IDLE",
            font_size='11sp',
            color=(0.6, 0.7, 0.8, 1.0),
            size_hint_y=None,
            height=dp(20),
            halign='center'
        )
        telemetry_card.add_widget(self.status_label)
        telemetry_card.add_widget(self.price_label)
        telemetry_card.add_widget(self.sma_label)
        root.add_widget(telemetry_card)

        # 3. Credentials & Settings Input Form
        config_card = DarkCard(
            orientation='vertical',
            size_hint_y=None,
            height=dp(155),
            padding=[dp(12), dp(10)],
            spacing=dp(6)
        )

        grid = GridLayout(cols=2, spacing=dp(8), size_hint_y=1.0)

        # App ID Field
        grid.add_widget(Label(text="Deriv App ID:", font_size='12sp', color=(0.8, 0.85, 0.9, 1), size_hint_x=0.38, halign='left'))
        self.input_app_id = TextInput(
            text="1089",  # Default standard Deriv app_id
            multiline=False,
            font_size='13sp',
            background_color=(0.12, 0.15, 0.22, 1.0),
            foreground_color=(1, 1, 1, 1),
            cursor_color=(0.95, 0.7, 0.1, 1),
            padding=[dp(8), dp(6)]
        )
        grid.add_widget(self.input_app_id)

        # API Token Field
        grid.add_widget(Label(text="API Token:", font_size='12sp', color=(0.8, 0.85, 0.9, 1), size_hint_x=0.38, halign='left'))
        self.input_token = TextInput(
            text="",
            password=True,
            hint_text="Enter Deriv API Token",
            hint_text_color=(0.4, 0.45, 0.55, 1),
            multiline=False,
            font_size='13sp',
            background_color=(0.12, 0.15, 0.22, 1.0),
            foreground_color=(1, 1, 1, 1),
            cursor_color=(0.95, 0.7, 0.1, 1),
            padding=[dp(8), dp(6)]
        )
        grid.add_widget(self.input_token)

        # Stake Amount Field
        grid.add_widget(Label(text="Stake (USD):", font_size='12sp', color=(0.8, 0.85, 0.9, 1), size_hint_x=0.38, halign='left'))
        self.input_amount = TextInput(
            text="10.0",
            multiline=False,
            font_size='13sp',
            background_color=(0.12, 0.15, 0.22, 1.0),
            foreground_color=(1, 1, 1, 1),
            cursor_color=(0.95, 0.7, 0.1, 1),
            padding=[dp(8), dp(6)]
        )
        grid.add_widget(self.input_amount)

        config_card.add_widget(grid)

        # Connection & Bot Toggle Row
        bot_ctrl_row = BoxLayout(orientation='horizontal', spacing=dp(8), size_hint_y=None, height=dp(36))
        self.btn_connect = Button(
            text="Start Bot",
            background_normal='',
            background_color=(0.1, 0.6, 0.35, 1.0),
            color=(1, 1, 1, 1),
            bold=True,
            font_size='13sp'
        )
        self.btn_connect.bind(on_press=self.toggle_connection)
        bot_ctrl_row.add_widget(self.btn_connect)

        self.btn_auto_algo = Button(
            text="Auto-Strategy: OFF",
            background_normal='',
            background_color=(0.3, 0.35, 0.45, 1.0),
            color=(1, 1, 1, 1),
            font_size='12sp'
        )
        self.btn_auto_algo.bind(on_press=self.toggle_algo)
        bot_ctrl_row.add_widget(self.btn_auto_algo)

        config_card.add_widget(bot_ctrl_row)
        root.add_widget(config_card)

        # 4. Emergency Manual Execution Buttons
        manual_box = BoxLayout(orientation='horizontal', spacing=dp(10), size_hint_y=None, height=dp(52))
        
        self.btn_buy = Button(
            text="[b]EMERGENCY BUY\n(CALL)[/b]",
            markup=True,
            background_normal='',
            background_color=(0.06, 0.65, 0.35, 1.0),
            color=(1, 1, 1, 1),
            font_size='13sp'
        )
        self.btn_buy.bind(on_press=lambda inst: self.send_order("CALL", "Manual Emergency BUY"))
        
        self.btn_sell = Button(
            text="[b]EMERGENCY SELL\n(PUT)[/b]",
            markup=True,
            background_normal='',
            background_color=(0.85, 0.20, 0.20, 1.0),
            color=(1, 1, 1, 1),
            font_size='13sp'
        )
        self.btn_sell.bind(on_press=lambda inst: self.send_order("PUT", "Manual Emergency SELL"))

        manual_box.add_widget(self.btn_buy)
        manual_box.add_widget(self.btn_sell)
        root.add_widget(manual_box)

        # 5. Live Log Terminal Window
        log_card = DarkCard(
            orientation='vertical',
            padding=[dp(10), dp(8)],
            spacing=dp(4),
            bg_color=(0.05, 0.07, 0.10, 1.0),
            border_color=(0.18, 0.22, 0.32, 0.8)
        )
        log_header = Label(
            text="[b]LIVE ACTIVITY & EXECUTION TICKETS[/b]",
            markup=True,
            font_size='11sp',
            color=(0.55, 0.65, 0.80, 1.0),
            size_hint_y=None,
            height=dp(18),
            halign='left'
        )
        log_card.add_widget(log_header)

        self.scroll = ScrollView(size_hint=(1, 1), bar_width=dp(4))
        self.log_label = Label(
            text="[System Ready] Configure App ID & API Token and tap 'Start Bot'.\n",
            markup=True,
            size_hint_y=None,
            font_size='11sp',
            color=(0.8, 0.88, 0.95, 1.0),
            valign='top',
            halign='left'
        )
        self.log_label.bind(width=lambda inst, val: setattr(inst, 'text_size', (val, None)))
        self.log_label.bind(texture_size=lambda inst, val: setattr(inst, 'height', val[1]))
        self.scroll.add_widget(self.log_label)
        log_card.add_widget(self.scroll)

        root.add_widget(log_card)
        return root

    # ---------------- UI Safe Logging & Updates ---------------- #
    def add_log(self, message: str, level: str = "info"):
        """Append log message thread-safely via Kivy Clock."""
        t_str = datetime.now().strftime("%H:%M:%S")
        color_map = {
            "info": "93C5FD",
            "success": "34D399",
            "warning": "FBBF24",
            "error": "F87171",
            "trade": "F59E0B"
        }
        hex_col = color_map.get(level, "D1D5DB")
        formatted = f"[color={hex_col}][{t_str}] {message}[/color]\n"
        
        def _update(dt):
            self.log_label.text += formatted
            self.scroll.scroll_y = 0  # Auto scroll to bottom
        Clock.schedule_once(_update)

    def set_status(self, text: str, color_hex: str):
        """Update status label safely."""
        def _update(dt):
            self.status_label.text = f"[b]STATUS:[/b] [color={color_hex}]{text}[/color]"
        Clock.schedule_once(_update)

    # ---------------- Connection & Bot Controls ---------------- #
    def toggle_connection(self, instance):
        if not self.is_running:
            token = self.input_token.text.strip()
            if not token:
                self.add_log("API Token is required to start bot!", "error")
                return
            self.is_running = True
            self.btn_connect.text = "Stop Bot"
            self.btn_connect.background_color = (0.75, 0.15, 0.15, 1.0)
            self.set_status("Connecting...", "FBBF24")
            self.add_log("Initializing WebSocket connection...", "info")
            self.ws_thread = threading.Thread(target=self._ws_worker_loop, daemon=True)
            self.ws_thread.start()
        else:
            self.stop_bot()

    def toggle_algo(self, instance):
        self.is_bot_active = not self.is_bot_active
        if self.is_bot_active:
            self.btn_auto_algo.text = "Auto-Strategy: ON"
            self.btn_auto_algo.background_color = (0.1, 0.6, 0.35, 1.0)
            self.add_log("SMA Crossover Automated Strategy activated.", "success")
        else:
            self.btn_auto_algo.text = "Auto-Strategy: OFF"
            self.btn_auto_algo.background_color = (0.3, 0.35, 0.45, 1.0)
            self.add_log("Automated Strategy deactivated (Manual only).", "warning")

    def stop_bot(self):
        self.is_running = False
        self.is_authenticated = False
        self.set_status("Disconnected", "EF4444")
        self.btn_connect.text = "Start Bot"
        self.btn_connect.background_color = (0.1, 0.6, 0.35, 1.0)
        self.add_log("Bot stopped and disconnected.", "warning")
        if self.ws:
            try:
                self.ws.close()
            except Exception:
                pass
            self.ws = None

    # ---------------- WebSocket Background Worker ---------------- #
    def _ws_worker_loop(self):
        if websocket is None:
            self.add_log("websocket-client not installed. Please run: pip install websocket-client", "error")
            Clock.schedule_once(lambda dt: self.stop_bot())
            return

        app_id = self.input_app_id.text.strip() or "1089"
        url = f"wss://ws.derivws.com/websockets/v3?app_id={app_id}"

        while self.is_running:
            try:
                self.add_log(f"Connecting to Deriv WS (AppID: {app_id})...", "info")
                self.ws = websocket.WebSocketApp(
                    url,
                    on_open=self._on_ws_open,
                    on_message=self._on_ws_message,
                    on_error=self._on_ws_error,
                    on_close=self._on_ws_close
                )
                self.ws.run_forever(ping_interval=30, ping_timeout=10)
            except Exception as e:
                self.add_log(f"Connection exception: {e}", "error")

            if self.is_running:
                self.add_log("Reconnecting in 5 seconds...", "warning")
                time.sleep(5)
            else:
                break

    def _on_ws_open(self, ws):
        self.add_log("WebSocket link established. Authenticating...", "info")
        token = self.input_token.text.strip()
        auth_payload = {"authorize": token}
        ws.send(json.dumps(auth_payload))

    def _on_ws_close(self, ws, close_status_code, close_msg):
        self.is_authenticated = False
        if self.is_running:
            self.set_status("Reconnecting...", "FBBF24")
            self.add_log(f"WebSocket closed ({close_status_code}: {close_msg})", "warning")

    def _on_ws_error(self, ws, error):
        self.add_log(f"WebSocket error: {error}", "error")

    def _on_ws_message(self, ws, message):
        try:
            data = json.loads(message)
            msg_type = data.get("msg_type")

            # 1. Error response handling
            if "error" in data:
                err_msg = data["error"].get("message", "Unknown Deriv error")
                self.add_log(f"Deriv API Error: {err_msg}", "error")
                if msg_type == "authorize":
                    self.set_status("Auth Failed", "EF4444")
                return

            # 2. Authorization success
            if msg_type == "authorize":
                self.is_authenticated = True
                auth_info = data.get("authorize", {})
                login_id = auth_info.get("loginid", "N/A")
                balance = auth_info.get("balance", 0.0)
                currency = auth_info.get("currency", "USD")
                self.set_status("Authenticated", "10B981")
                self.add_log(f"Authorized as {login_id} | Balance: {balance:.2f} {currency}", "success")
                
                # Subscribe to ticks for XAUUSD (Gold)
                self.set_status("Scanning Market...", "3B82F6")
                tick_payload = {"ticks": self.symbol, "subscribe": 1}
                ws.send(json.dumps(tick_payload))
                self.add_log(f"Subscribed to live ticks for {self.symbol}", "info")
                return

            # 3. Tick update handling
            if msg_type == "tick":
                tick_data = data.get("tick", {})
                quote = float(tick_data.get("quote", 0.0))
                self._handle_tick_update(quote)
                return

            # 4. Buy contract execution confirmation
            if msg_type == "buy":
                buy_data = data.get("buy", {})
                contract_id = buy_data.get("contract_id")
                buy_price = buy_data.get("buy_price")
                balance_after = buy_data.get("balance_after")
                self.add_log(f"ORDER EXECUTED! Ticket #{contract_id} | Cost: ${buy_price:.2f} | Balance: ${balance_after:.2f}", "success")
                return

        except Exception as e:
            self.add_log(f"Error parsing message: {e}", "error")

    def _handle_tick_update(self, quote: float):
        self.current_price = quote
        self.price_history.append(quote)

        # Update UI price label
        def _update_price(dt):
            self.price_label.text = f"[color=9CA3AF]XAUUSD:[/color] [b][color=FBBF24]{quote:.3f}[/color][/b] USD"
        Clock.schedule_once(_update_price)

        # Mathematical Simple Moving Average (SMA) Calculation
        if len(self.price_history) >= self.slow_period:
            prices = list(self.price_history)
            fast_sma = sum(prices[-self.fast_period:]) / self.fast_period
            slow_sma = sum(prices[-self.slow_period:]) / self.slow_period

            signal_txt = "NEUTRAL"
            if fast_sma > slow_sma:
                signal_txt = "[color=34D399]BULLISH (BUY)[/color]"
            elif fast_sma < slow_sma:
                signal_txt = "[color=F87171]BEARISH (SELL)[/color]"

            def _update_sma(dt):
                self.sma_label.text = f"Fast SMA({self.fast_period}): {fast_sma:.2f} | Slow SMA({self.slow_period}): {slow_sma:.2f} | {signal_txt}"
            Clock.schedule_once(_update_sma)

            # Automated Strategy Evaluation (Crossover)
            if self.is_bot_active:
                now = time.time()
                if now - self.last_trade_time > self.min_trade_interval_sec:
                    if fast_sma > slow_sma and self.last_signal != "CALL":
                        self.last_signal = "CALL"
                        self.last_trade_time = now
                        self.add_log("Algorithmic Signal: Fast SMA crossed above Slow SMA -> Triggering AUTO-BUY", "trade")
                        self.send_order("CALL", "SMA Auto Strategy")
                    elif fast_sma < slow_sma and self.last_signal != "PUT":
                        self.last_signal = "PUT"
                        self.last_trade_time = now
                        self.add_log("Algorithmic Signal: Fast SMA crossed below Slow SMA -> Triggering AUTO-SELL", "trade")
                        self.send_order("PUT", "SMA Auto Strategy")

    # ---------------- Order Execution ---------------- #
    def send_order(self, contract_type: str, reason: str = "Order"):
        if not self.ws or not self.is_authenticated:
            self.add_log(f"Cannot execute {contract_type}: Bot is not authenticated.", "error")
            return

        try:
            amount = float(self.input_amount.text.strip() or "10.0")
        except ValueError:
            self.add_log("Invalid stake amount specified!", "error")
            return

        order_payload = {
            "buy": 1,
            "price": 100000,
            "parameters": {
                "amount": amount,
                "basis": "stake",
                "contract_type": contract_type,  # "CALL" for Buy, "PUT" for Sell
                "currency": "USD",
                "symbol": self.symbol
            }
        }

        self.add_log(f"Sending {reason} [{contract_type}] Stake: ${amount:.2f} USD...", "trade")
        try:
            self.ws.send(json.dumps(order_payload))
        except Exception as e:
            self.add_log(f"Failed to send order payload: {e}", "error")

    def on_stop(self):
        """Clean shutdown when app is closed."""
        self.stop_bot()


if __name__ == "__main__":
    DerivTradingBotApp().run()
