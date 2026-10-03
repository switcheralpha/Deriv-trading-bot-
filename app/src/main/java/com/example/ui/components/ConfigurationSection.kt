package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BotStatus
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeBuyGreen
import com.example.ui.theme.TradeSellRed

@Composable
fun ConfigurationSection(
    appId: String,
    onAppIdChange: (String) -> Unit,
    apiToken: String,
    onApiTokenChange: (String) -> Unit,
    stakeAmount: String,
    onStakeAmountChange: (String) -> Unit,
    botStatus: BotStatus,
    onToggleBot: () -> Unit,
    isAutoAlgoEnabled: Boolean,
    onToggleAutoAlgo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isTokenVisible by remember { mutableStateOf(false) }
    val isRunning = botStatus !is BotStatus.Disconnected && botStatus !is BotStatus.Error

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BOT CREDENTIALS & RISK CONFIG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        letterSpacing = 0.5.sp
                    )
                }

                // Quick Demo Token filler helper button for fast evaluation
                Text(
                    text = "Demo Token Tip",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Deriv App ID & Stake Amount in one row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = appId,
                    onValueChange = onAppIdChange,
                    label = { Text("Deriv App ID", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(0.42f)
                        .testTag("input_app_id"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedLabelColor = GoldLight,
                        unfocusedLabelColor = TextSecondary
                    )
                )

                OutlinedTextField(
                    value = stakeAmount,
                    onValueChange = onStakeAmountChange,
                    label = { Text("Stake (USD)", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(0.58f)
                        .testTag("input_stake_amount"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedLabelColor = GoldLight,
                        unfocusedLabelColor = TextSecondary
                    )
                )
            }

            // Quick Stake preset buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("5", "10", "25", "50", "100").forEach { preset ->
                    val isSelected = stakeAmount == preset || stakeAmount == "$preset.0"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) GoldPrimary else DarkSurfaceElevated)
                            .clickable { onStakeAmountChange(preset) }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$$preset",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextSecondary
                        )
                    }
                }
            }

            // API Token Input Field
            OutlinedTextField(
                value = apiToken,
                onValueChange = onApiTokenChange,
                label = { Text("Deriv API Token", fontSize = 11.sp) },
                singleLine = true,
                visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                        Icon(
                            imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isTokenVisible) "Hide token" else "Show token",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                placeholder = { Text("Paste your Deriv API token here", fontSize = 11.sp, color = TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_api_token"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = DarkSurfaceBorder,
                    focusedLabelColor = GoldLight,
                    unfocusedLabelColor = TextSecondary
                )
            )

            // Start Bot & Auto-Strategy toggle buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onToggleBot,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("button_toggle_bot"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) TradeSellRed else TradeBuyGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "STOP BOT" else "START BOT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onToggleAutoAlgo,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(44.dp)
                        .testTag("button_toggle_auto_strategy"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isAutoAlgoEnabled) GoldPrimary else DarkSurfaceBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isAutoAlgoEnabled) GoldPrimary.copy(alpha = 0.15f) else Color.Transparent
                    )
                ) {
                    Text(
                        text = if (isAutoAlgoEnabled) "SMA ALGO: ON" else "SMA ALGO: OFF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isAutoAlgoEnabled) GoldLight else TextSecondary
                    )
                }
            }
        }
    }
}
