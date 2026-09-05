package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PredictedRecurringBill
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldBorder
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSavingsGreen
import com.example.ui.theme.SavioSavingsGreenBg
import com.example.ui.theme.SavioSlateBody
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import com.example.ui.theme.SavioSpendRose
import com.example.ui.theme.SavioSpendRoseBg
import com.example.ui.theme.SavioTransferIndigo
import com.example.ui.theme.SavioTransferIndigoBg
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringCommitmentsSheet(
    bills: List<PredictedRecurringBill>,
    currency: String,
    onDismiss: () -> Unit,
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

    val cal = remember { Calendar.getInstance() }
    val currentDay = cal.get(Calendar.DAY_OF_MONTH)

    val upcomingBills = remember(bills) { bills.filter { !it.isPaidThisMonth } }
    val paidBills = remember(bills) { bills.filter { it.isPaidThisMonth } }

    val upcomingTotal = remember(upcomingBills) { upcomingBills.sumOf { it.expectedAmount } }
    val paidTotal = remember(paidBills) { paidBills.sumOf { it.expectedAmount } }
    val totalRecurring = upcomingTotal + paidTotal

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = GlassBackground,
        modifier = modifier
            .navigationBarsPadding()
            .testTag("recurring_commitments_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SavioEmeraldContainer,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SavioEmeraldBorder),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EventRepeat,
                                    contentDescription = null,
                                    tint = SavioEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Subscription & Bill Radar",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SavioSlateDark
                            )
                            Text(
                                text = "Intelligent tracking of recurring commitments",
                                style = MaterialTheme.typography.bodySmall,
                                color = SavioSlateMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SavioSlateMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Summary Metrics Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Reserved Due",
                                style = MaterialTheme.typography.labelSmall,
                                color = SavioSlateMuted
                            )
                            Text(
                                text = "$currency${numberFormatter.format(upcomingTotal)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (upcomingTotal > 0) Color(0xFFD97706) else SavioSlateDark
                                )
                            )
                            Text(
                                text = "${upcomingBills.size} bill(s) pending",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = SavioSlateMuted
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Settled This Month",
                                style = MaterialTheme.typography.labelSmall,
                                color = SavioSlateMuted
                            )
                            Text(
                                text = "$currency${numberFormatter.format(paidTotal)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = SavioSavingsGreen
                                )
                            )
                            Text(
                                text = "${paidBills.size} bill(s) paid",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = SavioSlateMuted
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Outlay",
                                style = MaterialTheme.typography.labelSmall,
                                color = SavioSlateMuted
                            )
                            Text(
                                text = "$currency${numberFormatter.format(totalRecurring)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = SavioSlateDark
                                )
                            )
                            Text(
                                text = "${bills.size} detected",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = SavioSlateMuted
                            )
                        }
                    }
                }
            }

            // Section 1: Upcoming Bills (Due)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming & Due This Month (${upcomingBills.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark
                    )
                }
            }

            if (upcomingBills.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = GlassCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SavioEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "All detected recurring commitments are paid for this month! 🎉",
                                style = MaterialTheme.typography.bodySmall,
                                color = SavioSlateBody
                            )
                        }
                    }
                }
            } else {
                items(upcomingBills, key = { it.merchant }) { bill ->
                    val diff = bill.typicalDayOfMonth - currentDay
                    val dueBadgeText = when {
                        diff == 0 -> "Due Today!"
                        diff == 1 -> "Due Tomorrow"
                        diff in 2..5 -> "Due in $diff days (~${bill.typicalDayOfMonth}th)"
                        diff > 5 -> "Expected ~${bill.typicalDayOfMonth}th"
                        else -> "Due ~${bill.typicalDayOfMonth}th (Awaiting confirmation)"
                    }
                    val dueBadgeColor = when {
                        diff <= 0 -> SavioSpendRose
                        diff in 1..3 -> Color(0xFFD97706)
                        else -> SavioTransferIndigo
                    }
                    val dueBadgeBg = when {
                        diff <= 0 -> SavioSpendRoseBg
                        diff in 1..3 -> Color(0xFFFEF3C7)
                        else -> SavioTransferIndigoBg
                    }

                    RecurringBillItemCard(
                        bill = bill,
                        currency = currency,
                        numberFormatter = numberFormatter,
                        statusText = dueBadgeText,
                        badgeColor = dueBadgeColor,
                        badgeBg = dueBadgeBg,
                        isPaid = false,
                        onToggleRecurring = onToggleRecurring
                    )
                }
            }

            // Section 2: Paid Bills
            if (paidBills.isNotEmpty()) {
                item {
                    Text(
                        text = "Settled This Month (${paidBills.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                items(paidBills, key = { it.merchant }) { bill ->
                    val sdf = remember { SimpleDateFormat("dd MMM", Locale.US) }
                    val paidDateStr = if (bill.lastPaidTimestamp > 0) "Paid on ${sdf.format(Date(bill.lastPaidTimestamp))}" else "Paid this month"

                    RecurringBillItemCard(
                        bill = bill,
                        currency = currency,
                        numberFormatter = numberFormatter,
                        statusText = paidDateStr,
                        badgeColor = SavioSavingsGreen,
                        badgeBg = SavioSavingsGreenBg,
                        isPaid = true,
                        onToggleRecurring = onToggleRecurring
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringBillItemCard(
    bill: PredictedRecurringBill,
    currency: String,
    numberFormatter: NumberFormat,
    statusText: String,
    badgeColor: Color,
    badgeBg: Color,
    isPaid: Boolean,
    onToggleRecurring: ((merchant: String, isRecurring: Boolean) -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPaid) SavioSavingsGreenBg else badgeBg,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPaid) Icons.Default.Check else getCategoryIcon(bill.category),
                            contentDescription = bill.category,
                            tint = if (isPaid) SavioSavingsGreen else badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = bill.merchant,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = bill.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = SavioSlateMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currency${numberFormatter.format(bill.expectedAmount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isPaid) SavioSavingsGreen else SavioSlateDark
                    )
                )

                if (onToggleRecurring != null) {
                    Text(
                        text = if (bill.isManuallyMarked) "Tracked (Custom)" else "Auto-detected",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SavioSlateMuted
                    )
                }
            }
        }
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    val norm = category.lowercase(Locale.US)
    return when {
        norm.contains("food") || norm.contains("dining") -> Icons.Default.Fastfood
        norm.contains("grocery") || norm.contains("supermarket") -> Icons.Default.ShoppingBag
        norm.contains("shopping") -> Icons.Default.Storefront
        norm.contains("travel") || norm.contains("flight") -> Icons.Default.Flight
        norm.contains("bill") || norm.contains("utility") || norm.contains("electric") -> Icons.AutoMirrored.Filled.ReceiptLong
        norm.contains("transfer") -> Icons.Default.SwapHoriz
        else -> Icons.Default.Receipt
    }
}
