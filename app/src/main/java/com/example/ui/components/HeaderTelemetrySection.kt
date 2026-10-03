package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AccountInfo
import com.example.data.model.BotStatus
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeBuyGreen
import com.example.ui.theme.TradeSellRed

@Composable
fun HeaderTelemetrySection(
    status: BotStatus,
    currentPrice: Double?,
    priceChange: Double,
    priceHistory: List<Double>,
    fastSma: Double?,
    slowSma: Double?,
    signal: String,
    accountInfo: AccountInfo?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Banner Card with branding
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.gold_trading_hero),
                    contentDescription = "Gold Trading Hero Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.42f
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0B0E14).copy(alpha = 0.95f),
                                    Color(0xFF0B0E14).copy(alpha = 0.60f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (status) {
                                            is BotStatus.ScanningMarket -> TradeBuyGreen
                                            is BotStatus.Authenticated -> CyanTelemetry
                                            is BotStatus.Connecting -> GoldLight
                                            else -> TradeSellRed
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DERIV ALGO BOT",
                                color = GoldLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "XAUUSD (Gold) Engine",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "High-Frequency WebSocket Terminal",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Account balance chip if authenticated
                    if (accountInfo != null) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2E)),
                            modifier = Modifier.border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = accountInfo.loginId,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "$%.2f %s".format(accountInfo.balance, accountInfo.currency),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Market Status & Large Price Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_telemetry_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "STATUS: ",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        val statusColor = when (status) {
                            is BotStatus.ScanningMarket -> TradeBuyGreen
                            is BotStatus.Authenticated -> CyanTelemetry
                            is BotStatus.Connecting -> GoldLight
                            else -> TradeSellRed
                        }
                        Text(
                            text = status.toString(),
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("label_status")
                        )
                    }

                    // Signal Badge
                    val isBullish = signal.contains("BUY")
                    val isBearish = signal.contains("SELL")
                    val badgeColor = when {
                        isBullish -> TradeBuyGreen
                        isBearish -> TradeSellRed
                        else -> CyanTelemetry
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = signal,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Price and Delta row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "XAUUSD LIVE SPOT",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            val priceTextColor by animateColorAsState(
                                targetValue = when {
                                    priceChange > 0 -> TradeBuyGreen
                                    priceChange < 0 -> TradeSellRed
                                    else -> GoldLight
                                },
                                animationSpec = tween(durationMillis = 400),
                                label = "price_color"
                            )

                            Text(
                                text = currentPrice?.let { "$%.3f".format(it) } ?: "---.---",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = priceTextColor,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("label_gold_price")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "USD",
                                fontSize = 12.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    // Delta indicator
                    if (currentPrice != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (priceChange >= 0) TradeBuyGreen.copy(alpha = 0.15f)
                                    else TradeSellRed.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (priceChange >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (priceChange >= 0) TradeBuyGreen else TradeSellRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "%+.3f".format(priceChange),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (priceChange >= 0) TradeBuyGreen else TradeSellRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Mini Canvas Chart for Price Ticks & SMA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF090D14))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    if (priceHistory.size > 2) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val minVal = priceHistory.minOrNull() ?: 0.0
                            val maxVal = priceHistory.maxOrNull() ?: 1.0
                            val range = (maxVal - minVal).coerceAtLeast(0.001)

                            val stepX = size.width / (priceHistory.size - 1).coerceAtLeast(1)
                            val path = Path()

                            priceHistory.forEachIndexed { i, price ->
                                val x = i * stepX
                                val y = size.height - (((price - minVal) / range) * size.height).toFloat()
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(
                                path = path,
                                color = GoldPrimary,
                                style = Stroke(width = 2.5f)
                            )

                            // Draw last point indicator
                            val lastPrice = priceHistory.last()
                            val lastX = size.width
                            val lastY = size.height - (((lastPrice - minVal) / range) * size.height).toFloat()
                            drawCircle(
                                color = GoldLight,
                                radius = 4f,
                                center = Offset(lastX, lastY)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Awaiting ticks to generate live curve...",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // SMA Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Fast SMA(5): ${fastSma?.let { "%.2f".format(it) } ?: "--"}",
                        color = CyanTelemetry,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Slow SMA(15): ${slowSma?.let { "%.2f".format(it) } ?: "--"}",
                        color = GoldLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
