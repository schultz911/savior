package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseEntity
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioBlacklistBg
import com.example.ui.theme.SavioBlacklistMuted
import com.example.ui.theme.SavioBlacklistRed
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldBorder
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSlateBody
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import com.example.ui.theme.SavioSpendRose
import com.example.ui.theme.SavioSpendRoseBg
import com.example.ui.theme.SavioTransferIndigo
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthSpendPoint(
    val monthKey: String,
    val label: String,
    val total: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantDetailSheet(
    merchantName: String,
    currency: String,
    isBlacklisted: Boolean,
    expenses: List<ExpenseEntity>,
    onDismiss: () -> Unit,
    onToggleBlacklist: (String) -> Unit,
    onToggleRecurring: ((merchant: String, isRecurring: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    val totalLifetimeSpend = remember(expenses) { expenses.sumOf { it.amount } }
    val orderCount = expenses.size
    val averageTicketSize = remember(expenses) {
        if (orderCount > 0) totalLifetimeSpend / orderCount else 0.0
    }
    val isAnyRecurring = remember(expenses) { expenses.any { it.isRecurring } }
    val primaryCategory = remember(expenses) {
        expenses.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key ?: "General Spend"
    }

    // Generate 6-month historical sparkline data
    val sparklinePoints = remember(expenses) {
        val list = mutableListOf<MonthSpendPoint>()
        val cal = Calendar.getInstance()
        val sdfKey = SimpleDateFormat("yyyy-MM", Locale.US)
        val sdfLabel = SimpleDateFormat("MMM", Locale.US)

        for (i in 5 downTo 0) {
            val c = Calendar.getInstance().apply {
                time = cal.time
                add(Calendar.MONTH, -i)
            }
            val key = sdfKey.format(c.time)
            val lbl = sdfLabel.format(c.time)
            val monthTotal = expenses.filter { it.monthKey == key }.sumOf { it.amount }
            list.add(MonthSpendPoint(key, lbl, monthTotal))
        }
        list
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GlassBackground,
        dragHandle = null,
        modifier = modifier.testTag("merchant_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBlacklisted) SavioBlacklistBg else SavioEmeraldContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBlacklisted) Icons.Default.Block else Icons.Default.Storefront,
                            contentDescription = "Merchant Icon",
                            tint = if (isBlacklisted) SavioBlacklistRed else SavioEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = merchantName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                textDecoration = if (isBlacklisted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (isBlacklisted) SavioBlacklistMuted else SavioSlateDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SavioEmeraldContainer
                            ) {
                                Text(
                                    text = primaryCategory,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SavioEmerald
                                )
                            }

                            if (isAnyRecurring) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "Recurring",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SavioSlateDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Summary Grid (Lifetime, Order count, Avg Ticket)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricPill(
                    label = "Lifetime Spend",
                    value = "$currency${numberFormatter.format(totalLifetimeSpend)}",
                    accentColor = SavioSlateDark,
                    modifier = Modifier.weight(1f)
                )

                MetricPill(
                    label = "Transactions",
                    value = "$orderCount",
                    accentColor = SavioEmerald,
                    modifier = Modifier.weight(0.7f)
                )

                MetricPill(
                    label = "Avg Ticket",
                    value = "$currency${numberFormatter.format(averageTicketSize)}",
                    accentColor = SavioTransferIndigo,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6-Month Spend Trend Micro-Sparkline
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = GlassCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = SavioEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "6-Month Spend Velocity",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SavioSlateDark
                            )
                        }

                        Text(
                            text = "Peak: $currency${numberFormatter.format(sparklinePoints.maxOfOrNull { it.total } ?: 0.0)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SavioSlateMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    MerchantSparkline(
                        points = sparklinePoints,
                        lineColor = SavioEmerald,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Month Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (pt in sparklinePoints) {
                            Text(
                                text = pt.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = SavioSlateMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vendor Control Quick Actions (Recurring Toggle & Blacklist Toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onToggleRecurring?.invoke(merchantName, !isAnyRecurring)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isAnyRecurring) Color(0xFF2563EB) else SavioSlateDark
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAnyRecurring) Color(0xFF93C5FD) else GlassCardBorder
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventRepeat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAnyRecurring) "Recurring" else "Mark Recurring",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = { onToggleBlacklist(merchantName) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isBlacklisted) SavioBlacklistRed else SavioSlateDark
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isBlacklisted) SavioBlacklistRed.copy(alpha = 0.4f) else GlassCardBorder
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isBlacklisted) Icons.Default.Check else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBlacklisted) "Unblacklist" else "Blacklist",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Order History Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = SavioSlateDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Transaction History (${expenses.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = SavioSlateDark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Recent History List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(expenses, key = { it.id }) { item ->
                    MerchantHistoryRow(
                        item = item,
                        currency = currency,
                        numberFormatter = numberFormatter
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = GlassCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = SavioSlateMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                ),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MerchantSparkline(
    points: List<MonthSpendPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val maxVal = points.maxOfOrNull { it.total }?.coerceAtLeast(1.0) ?: 1.0
        val width = size.width
        val height = size.height
        val stepX = if (points.size > 1) width / (points.size - 1) else width

        val coordinates = points.mapIndexed { index, pt ->
            val x = index * stepX
            val normalizedY = (pt.total / maxVal).toFloat()
            val y = height - (normalizedY * (height - 16f)) - 8f
            Offset(x, y)
        }

        // Draw smooth line
        val linePath = Path()
        val fillPath = Path()

        coordinates.forEachIndexed { i, pt ->
            if (i == 0) {
                linePath.moveTo(pt.x, pt.y)
                fillPath.moveTo(pt.x, height)
                fillPath.lineTo(pt.x, pt.y)
            } else {
                val prev = coordinates[i - 1]
                val midX = (prev.x + pt.x) / 2f
                linePath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                fillPath.cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
            }
        }
        fillPath.lineTo(coordinates.last().x, height)
        fillPath.close()

        // Fill gradient under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.25f),
                    lineColor.copy(alpha = 0.02f)
                )
            )
        )

        // Stroke line
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Point dots
        coordinates.forEach { pt ->
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = lineColor,
                radius = 2.5.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
private fun MerchantHistoryRow(
    item: ExpenseEntity,
    currency: String,
    numberFormatter: NumberFormat
) {
    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(item.timestamp))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SavioEmerald
                    )
                    if (item.isRecurring) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• Recurring",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF2563EB)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = SavioSlateMuted
                )
            }

            Text(
                text = "-$currency${numberFormatter.format(item.amount)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = SavioSlateDark
            )
        }
    }
}
