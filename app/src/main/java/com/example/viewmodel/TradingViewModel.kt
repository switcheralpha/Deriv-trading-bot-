package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.deriv.DerivWebSocketManager
import com.example.data.model.AccountInfo
import com.example.data.model.BotStatus
import com.example.data.model.LogEntry
import com.example.data.model.LogLevel
import com.example.data.model.TradeTicket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TradingViewModel : ViewModel() {

    private val _appId = MutableStateFlow("1089")
    val appId: StateFlow<String> = _appId.asStateFlow()

    private val _apiToken = MutableStateFlow("")
    val apiToken: StateFlow<String> = _apiToken.asStateFlow()

    private val _stakeAmount = MutableStateFlow("10.0")
    val stakeAmount: StateFlow<String> = _stakeAmount.asStateFlow()

    private val _botStatus = MutableStateFlow<BotStatus>(BotStatus.Disconnected)
    val botStatus: StateFlow<BotStatus> = _botStatus.asStateFlow()

    private val _accountInfo = MutableStateFlow<AccountInfo?>(null)
    val accountInfo: StateFlow<AccountInfo?> = _accountInfo.asStateFlow()

    private val _currentPrice = MutableStateFlow<Double?>(null)
    val currentPrice: StateFlow<Double?> = _currentPrice.asStateFlow()

    private val _priceChange = MutableStateFlow(0.0)
    val priceChange: StateFlow<Double> = _priceChange.asStateFlow()

    private val _priceHistory = MutableStateFlow<List<Double>>(emptyList())
    val priceHistory: StateFlow<List<Double>> = _priceHistory.asStateFlow()

    private val _fastSma = MutableStateFlow<Double?>(null)
    val fastSma: StateFlow<Double?> = _fastSma.asStateFlow()

    private val _slowSma = MutableStateFlow<Double?>(null)
    val slowSma: StateFlow<Double?> = _slowSma.asStateFlow()

    private val _marketSignal = MutableStateFlow("NEUTRAL")
    val marketSignal: StateFlow<String> = _marketSignal.asStateFlow()

    private val _isAutoAlgoEnabled = MutableStateFlow(false)
    val isAutoAlgoEnabled: StateFlow<Boolean> = _isAutoAlgoEnabled.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(
        listOf(
            LogEntry(
                timestamp = currentTimeFormatted(),
                message = "Trading engine initialized. Enter credentials and tap 'Start Bot'.",
                level = LogLevel.INFO
            )
        )
    )
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _recentTickets = MutableStateFlow<List<TradeTicket>>(emptyList())
    val recentTickets: StateFlow<List<TradeTicket>> = _recentTickets.asStateFlow()

    private var previousQuote: Double? = null
    private var lastSignalType: String? = null
    private var lastTradeTimestamp: Long = 0L
    private val minTradeIntervalMs: Long = 15_000L // 15 seconds cooldown

    private val derivManager = DerivWebSocketManager(
        onStatusChanged = { status ->
            _botStatus.value = status
        },
        onLog = { msg, level ->
            appendLog(msg, level)
        },
        onAuthorized = { info ->
            _accountInfo.value = info
        },
        onTick = { quote, epoch, symbol ->
            handleIncomingTick(quote)
        },
        onTradeExecuted = { ticket ->
            _recentTickets.value = listOf(ticket) + _recentTickets.value.take(9)
            // Update account balance if known
            val currentAcc = _accountInfo.value
            if (currentAcc != null) {
                _accountInfo.value = currentAcc.copy(balance = ticket.balanceAfter)
            }
        }
    )

    fun onAppIdChange(newId: String) {
        _appId.value = newId
    }

    fun onApiTokenChange(newToken: String) {
        _apiToken.value = newToken
    }

    fun onStakeAmountChange(newAmount: String) {
        _stakeAmount.value = newAmount
    }

    fun toggleBot() {
        val current = _botStatus.value
        if (current is BotStatus.Disconnected || current is BotStatus.Error) {
            derivManager.connect(_appId.value, _apiToken.value)
        } else {
            derivManager.disconnect()
            _botStatus.value = BotStatus.Disconnected
        }
    }

    fun toggleAutoAlgo() {
        val next = !_isAutoAlgoEnabled.value
        _isAutoAlgoEnabled.value = next
        if (next) {
            appendLog("Automated SMA Crossover execution enabled.", LogLevel.SUCCESS)
        } else {
            appendLog("Automated execution disabled (Manual Emergency Only).", LogLevel.WARNING)
        }
    }

    fun executeEmergencyBuy() {
        val amount = _stakeAmount.value.toDoubleOrNull() ?: 10.0
        derivManager.sendMarketOrder("CALL", amount, "Manual Emergency BUY")
    }

    fun executeEmergencySell() {
        val amount = _stakeAmount.value.toDoubleOrNull() ?: 10.0
        derivManager.sendMarketOrder("PUT", amount, "Manual Emergency SELL")
    }

    fun clearLogs() {
        _logs.value = listOf(
            LogEntry(
                timestamp = currentTimeFormatted(),
                message = "Log history cleared.",
                level = LogLevel.INFO
            )
        )
    }

    private fun handleIncomingTick(quote: Double) {
        viewModelScope.launch {
            val prev = previousQuote ?: quote
            _priceChange.value = quote - prev
            previousQuote = quote
            _currentPrice.value = quote

            val history = (_priceHistory.value + quote).takeLast(60)
            _priceHistory.value = history

            // Simple Moving Average Calculation
            val fastPeriod = 5
            val slowPeriod = 15

            if (history.size >= slowPeriod) {
                val fSma = history.takeLast(fastPeriod).average()
                val sSma = history.takeLast(slowPeriod).average()
                _fastSma.value = fSma
                _slowSma.value = sSma

                if (fSma > sSma) {
                    _marketSignal.value = "BULLISH (BUY)"
                    // Check automated execution
                    if (_isAutoAlgoEnabled.value && lastSignalType != "CALL") {
                        val now = System.currentTimeMillis()
                        if (now - lastTradeTimestamp > minTradeIntervalMs) {
                            lastSignalType = "CALL"
                            lastTradeTimestamp = now
                            appendLog("ALGO TRIGGER: Fast SMA ($fSma) crossed above Slow SMA ($sSma) -> BUY", LogLevel.TRADE)
                            val amount = _stakeAmount.value.toDoubleOrNull() ?: 10.0
                            derivManager.sendMarketOrder("CALL", amount, "Auto SMA Algo")
                        }
                    }
                } else if (fSma < sSma) {
                    _marketSignal.value = "BEARISH (SELL)"
                    // Check automated execution
                    if (_isAutoAlgoEnabled.value && lastSignalType != "PUT") {
                        val now = System.currentTimeMillis()
                        if (now - lastTradeTimestamp > minTradeIntervalMs) {
                            lastSignalType = "PUT"
                            lastTradeTimestamp = now
                            appendLog("ALGO TRIGGER: Fast SMA ($fSma) crossed below Slow SMA ($sSma) -> SELL", LogLevel.TRADE)
                            val amount = _stakeAmount.value.toDoubleOrNull() ?: 10.0
                            derivManager.sendMarketOrder("PUT", amount, "Auto SMA Algo")
                        }
                    }
                } else {
                    _marketSignal.value = "NEUTRAL"
                }
            } else {
                _marketSignal.value = "CALIBRATING (${history.size}/$slowPeriod TICKS)"
            }
        }
    }

    private fun appendLog(msg: String, level: LogLevel) {
        viewModelScope.launch {
            val entry = LogEntry(
                timestamp = currentTimeFormatted(),
                message = msg,
                level = level
            )
            _logs.value = (_logs.value + entry).takeLast(200)
        }
    }

    companion object {
        fun currentTimeFormatted(): String {
            return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        }
    }

    override fun onCleared() {
        super.onCleared()
        derivManager.disconnect()
    }
}
