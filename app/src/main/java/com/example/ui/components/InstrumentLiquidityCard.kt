package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.models.InstrumentSpendSummary
import com.example.ui.models.InstrumentType
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldBorder
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import com.example.ui.theme.SavioTransferIndigo
import com.example.ui.theme.SavioTransferIndigoBg
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InstrumentLiquidityCard(
    instruments: List<InstrumentSpendSummary>,
    selectedAccount: String?,
    onSelectAccount: (String?) -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (instruments.isEmpty()) return

    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCardBg),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
            .testTag("instrument_liquidity_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SavioEmeraldContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = SavioEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Instrument Intelligence",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = SavioSlateDark
                        )
                        Text(
                            text = "Tap to filter transactions by instrument",
                            style = MaterialTheme.typography.bodySmall,
                            color = SavioSlateMuted
                        )
                    }
                }

                if (selectedAccount != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SavioEmeraldContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SavioEmeraldBorder),
                        modifier = Modifier.clickable { onSelectAccount(null) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SavioEmerald
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear filter",
                                tint = SavioEmerald,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontally Scrollable Instrument Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                instruments.forEach { item ->
                    val isSelected = selectedAccount == item.accountInfo
                    val icon = when (item.instrumentType) {
                        InstrumentType.CARD -> Icons.Default.CreditCard
                        InstrumentType.UPI -> Icons.Default.SwapHoriz
                        InstrumentType.BANK_ACCOUNT -> Icons.Default.AccountBalance
                        InstrumentType.OTHER -> Icons.Default.Payments
                    }

                    val borderColor = if (isSelected) SavioEmerald else GlassCardBorder
                    val bgColor = if (isSelected) SavioEmeraldContainer.copy(alpha = 0.5f) else GlassBackground

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = bgColor,
                        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                        modifier = Modifier
                            .width(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectAccount(item.accountInfo) }
                            .testTag("instrument_chip_${item.accountInfo}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) SavioEmerald else SavioEmeraldContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else SavioEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) SavioEmerald else SavioEmeraldContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "${item.percentageOfTotal.toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = if (isSelected) Color.White else SavioEmerald,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.accountInfo,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = SavioSlateDark
                            )

                            Text(
                                text = "$currency${numberFormatter.format(item.totalSpent)} • ${item.transactionCount} txns",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = SavioSlateMuted
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { (item.percentageOfTotal / 100f).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (isSelected) SavioEmerald else SavioTransferIndigo,
                                trackColor = Color.LightGray.copy(alpha = 0.3f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}
