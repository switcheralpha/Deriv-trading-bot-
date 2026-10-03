package com.example.data.model

import java.util.UUID

sealed class BotStatus {
    object Disconnected : BotStatus() {
        override fun toString(): String = "Disconnected"
    }
    object Connecting : BotStatus() {
        override fun toString(): String = "Connecting..."
    }
    object Authenticated : BotStatus() {
        override fun toString(): String = "Authenticated"
    }
    object ScanningMarket : BotStatus() {
        override fun toString(): String = "Scanning Market..."
    }
    data class Error(val message: String) : BotStatus() {
        override fun toString(): String = "Error: $message"
    }
}

enum class LogLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    TRADE
}

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)

data class AccountInfo(
    val loginId: String,
    val balance: Double,
    val currency: String,
    val email: String = ""
)

data class TickData(
    val quote: Double,
    val epoch: Long,
    val symbol: String
)

data class TradeTicket(
    val contractId: Long,
    val contractType: String,
    val buyPrice: Double,
    val balanceAfter: Double,
    val timestamp: Long = System.currentTimeMillis()
)
