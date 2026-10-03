package com.example.data.deriv

import com.example.data.model.AccountInfo
import com.example.data.model.BotStatus
import com.example.data.model.LogLevel
import com.example.data.model.TradeTicket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DerivWebSocketManager(
    private val onStatusChanged: (BotStatus) -> Unit,
    private val onLog: (String, LogLevel) -> Unit,
    private val onAuthorized: (AccountInfo) -> Unit,
    private val onTick: (Double, Long, String) -> Unit,
    private val onTradeExecuted: (TradeTicket) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive for streaming
        .build()

    private var webSocket: WebSocket? = null
    private val isConnecting = AtomicBoolean(false)
    private val shouldStayConnected = AtomicBoolean(false)
    private val isAuthorized = AtomicBoolean(false)

    private var currentAppId: String = "1089"
    private var currentToken: String = ""
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var reconnectJob: Job? = null

    val symbol: String = "frxXAUUSD"

    fun connect(appId: String, apiToken: String) {
        currentAppId = appId.trim().ifEmpty { "1089" }
        currentToken = apiToken.trim()
        shouldStayConnected.set(true)

        if (currentToken.isEmpty()) {
            onLog("API Token is required to connect to Deriv.", LogLevel.ERROR)
            onStatusChanged(BotStatus.Error("Missing API Token"))
            return
        }

        reconnectJob?.cancel()
        initiateSocketConnection()
    }

    private fun initiateSocketConnection() {
        if (isConnecting.get()) return
        isConnecting.set(true)
        onStatusChanged(BotStatus.Connecting)
        onLog("Connecting to Deriv WebSocket (App ID: $currentAppId)...", LogLevel.INFO)

        val url = "wss://ws.derivws.com/websockets/v3?app_id=$currentAppId"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnecting.set(false)
                onLog("WebSocket connection established. Authorizing session...", LogLevel.INFO)
                // Step 1: Send authorization payload
                val authPayload = JSONObject().apply {
                    put("authorize", currentToken)
                }
                ws.send(authPayload.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text, ws)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                isAuthorized.set(false)
                onLog("WebSocket closing: $reason (code: $code)", LogLevel.WARNING)
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isAuthorized.set(false)
                onLog("WebSocket closed: $reason", LogLevel.WARNING)
                if (shouldStayConnected.get()) {
                    triggerReconnect()
                } else {
                    onStatusChanged(BotStatus.Disconnected)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnecting.set(false)
                isAuthorized.set(false)
                val errMsg = t.localizedMessage ?: "Network connection failed"
                onLog("WebSocket error: $errMsg", LogLevel.ERROR)
                onStatusChanged(BotStatus.Error(errMsg))
                if (shouldStayConnected.get()) {
                    triggerReconnect()
                }
            }
        })
    }

    private fun handleIncomingMessage(text: String, ws: WebSocket) {
        try {
            val json = JSONObject(text)

            // Check for API errors
            if (json.has("error")) {
                val errObj = json.getJSONObject("error")
                val errMsg = errObj.optString("message", "Deriv API returned an error")
                val errCode = errObj.optString("code", "")
                onLog("Deriv Error [$errCode]: $errMsg", LogLevel.ERROR)

                val msgType = json.optString("msg_type")
                if (msgType == "authorize") {
                    onStatusChanged(BotStatus.Error("Auth failed: $errMsg"))
                    isAuthorized.set(false)
                }
                return
            }

            val msgType = json.optString("msg_type")

            when (msgType) {
                "authorize" -> {
                    isAuthorized.set(true)
                    val authObj = json.getJSONObject("authorize")
                    val loginId = authObj.optString("loginid", "Unknown")
                    val balance = authObj.optDouble("balance", 0.0)
                    val currency = authObj.optString("currency", "USD")
                    val email = authObj.optString("email", "")

                    val accountInfo = AccountInfo(loginId, balance, currency, email)
                    onAuthorized(accountInfo)
                    onStatusChanged(BotStatus.Authenticated)
                    onLog("Authenticated as $loginId | Balance: $balance $currency", LogLevel.SUCCESS)

                    // Step 2: Subscribe to ticks for Gold
                    onStatusChanged(BotStatus.ScanningMarket)
                    val subscribePayload = JSONObject().apply {
                        put("ticks", symbol)
                        put("subscribe", 1)
                    }
                    ws.send(subscribePayload.toString())
                    onLog("Subscribed to live market ticks for Gold ($symbol)", LogLevel.INFO)
                }

                "tick" -> {
                    val tickObj = json.getJSONObject("tick")
                    val quote = tickObj.optDouble("quote", 0.0)
                    val epoch = tickObj.optLong("epoch", System.currentTimeMillis() / 1000)
                    val sym = tickObj.optString("symbol", symbol)
                    onTick(quote, epoch, sym)
                }

                "buy" -> {
                    val buyObj = json.getJSONObject("buy")
                    val contractId = buyObj.optLong("contract_id", 0L)
                    val buyPrice = buyObj.optDouble("buy_price", 0.0)
                    val balanceAfter = buyObj.optDouble("balance_after", 0.0)

                    val ticket = TradeTicket(
                        contractId = contractId,
                        contractType = "BUY",
                        buyPrice = buyPrice,
                        balanceAfter = balanceAfter
                    )
                    onTradeExecuted(ticket)
                    onLog(
                        "TRADE CONFIRMED: Ticket #$contractId | Cost: $$buyPrice | New Balance: $$balanceAfter",
                        LogLevel.TRADE
                    )
                }

                "ping" -> {
                    // Deriv ping response
                }
            }
        } catch (e: Exception) {
            onLog("Failed to parse incoming payload: ${e.localizedMessage}", LogLevel.ERROR)
        }
    }

    fun sendMarketOrder(contractType: String, amount: Double, reason: String = "Manual") {
        val ws = webSocket
        if (ws == null || !isAuthorized.get()) {
            onLog("Cannot execute $contractType order: Not authenticated with Deriv.", LogLevel.ERROR)
            return
        }

        try {
            val params = JSONObject().apply {
                put("amount", amount)
                put("basis", "stake")
                put("contract_type", contractType) // "CALL" for Buy, "PUT" for Sell
                put("currency", "USD")
                put("symbol", symbol)
            }

            val payload = JSONObject().apply {
                put("buy", 1)
                put("price", 100000) // Deriv standard max price buffer
                put("parameters", params)
            }

            onLog("Transmitting $reason [$contractType] order: Stake $$amount USD...", LogLevel.TRADE)
            ws.send(payload.toString())
        } catch (e: Exception) {
            onLog("Failed to transmit order: ${e.localizedMessage}", LogLevel.ERROR)
        }
    }

    private fun triggerReconnect() {
        if (!shouldStayConnected.get()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            onStatusChanged(BotStatus.Connecting)
            onLog("Reconnecting to Deriv in 5 seconds...", LogLevel.WARNING)
            delay(5000)
            if (shouldStayConnected.get()) {
                initiateSocketConnection()
            }
        }
    }

    fun disconnect() {
        shouldStayConnected.set(false)
        isAuthorized.set(false)
        isConnecting.set(false)
        reconnectJob?.cancel()
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        webSocket = null
        onStatusChanged(BotStatus.Disconnected)
        onLog("Disconnected from Deriv Broker.", LogLevel.WARNING)
    }
}
