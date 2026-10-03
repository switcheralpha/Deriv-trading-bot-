package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ConfigurationSection
import com.example.ui.components.EmergencyActionSection
import com.example.ui.components.HeaderTelemetrySection
import com.example.ui.components.LiveLogWindow
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.viewmodel.TradingViewModel

@Composable
fun TradingScreen(
    viewModel: TradingViewModel = viewModel()
) {
    val haptic = LocalHapticFeedback.current

    val appId by viewModel.appId.collectAsState()
    val apiToken by viewModel.apiToken.collectAsState()
    val stakeAmount by viewModel.stakeAmount.collectAsState()
    val botStatus by viewModel.botStatus.collectAsState()
    val accountInfo by viewModel.accountInfo.collectAsState()

    val currentPrice by viewModel.currentPrice.collectAsState()
    val priceChange by viewModel.priceChange.collectAsState()
    val priceHistory by viewModel.priceHistory.collectAsState()
    val fastSma by viewModel.fastSma.collectAsState()
    val slowSma by viewModel.slowSma.collectAsState()
    val signal by viewModel.marketSignal.collectAsState()
    val isAutoAlgoEnabled by viewModel.isAutoAlgoEnabled.collectAsState()

    val logs by viewModel.logs.collectAsState()

    var showHelpDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header & Live Market Telemetry Section
            HeaderTelemetrySection(
                status = botStatus,
                currentPrice = currentPrice,
                priceChange = priceChange,
                priceHistory = priceHistory,
                fastSma = fastSma,
                slowSma = slowSma,
                signal = signal,
                accountInfo = accountInfo
            )

            // 2. Risk & Credentials Configuration Section
            ConfigurationSection(
                appId = appId,
                onAppIdChange = viewModel::onAppIdChange,
                apiToken = apiToken,
                onApiTokenChange = viewModel::onApiTokenChange,
                stakeAmount = stakeAmount,
                onStakeAmountChange = viewModel::onStakeAmountChange,
                botStatus = botStatus,
                onToggleBot = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleBot()
                },
                isAutoAlgoEnabled = isAutoAlgoEnabled,
                onToggleAutoAlgo = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.toggleAutoAlgo()
                }
            )

            // 3. Emergency Manual Execution Section
            EmergencyActionSection(
                onEmergencyBuy = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.executeEmergencyBuy()
                },
                onEmergencySell = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.executeEmergencySell()
                },
                stakeAmount = stakeAmount
            )

            // 4. Live Activity & Execution Tickets Terminal Window
            LiveLogWindow(
                logs = logs,
                onClearLogs = viewModel::clearLogs
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Deriv WebSocket API Guide", color = GoldLight) },
            text = {
                Text(
                    "To generate a Deriv API Token:\n\n" +
                    "1. Log in to your Deriv account (app.deriv.com).\n" +
                    "2. Navigate to Account Settings -> API Token.\n" +
                    "3. Select 'Read' and 'Trade' scopes, then generate.\n" +
                    "4. Paste the token into the input field above.\n\n" +
                    "The bot connects over WebSocket (wss://ws.derivws.com) to stream real-time ticks for XAUUSD (Gold) and execute orders.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("OK", color = GoldPrimary)
                }
            }
        )
    }
}
