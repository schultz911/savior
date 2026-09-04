package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseEntity
import com.example.ui.models.MonthAnalytics
import com.example.ui.theme.BentoCardBg
import com.example.ui.theme.BentoCardBorder
import com.example.ui.theme.BentoDarkTile
import com.example.ui.theme.BentoDebitRed
import com.example.ui.theme.BentoLavenderCard
import com.example.ui.theme.BentoLavenderContainer
import com.example.ui.theme.BentoPurpleDark
import com.example.ui.theme.BentoPurplePrimary
import com.example.ui.theme.BentoSpendPlum
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.SavioSavingsGreen
import com.example.ui.theme.SavioSavingsGreenBg
import com.example.ui.theme.SavioSpendRed
import com.example.ui.theme.SavioSpendRedBg
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarAnalyticsTab(
    currency: String,
    monthlySalary: Double,
    savingsGoal: Double,
    selectedMonthKey: String,
    last12Months: List<MonthAnalytics>,
    currentMonthExpenses: List<ExpenseEntity>,
    onSelectMonth: (String) -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }

    val currentCal = remember { Calendar.getInstance() }
    val currentYear = currentCal.get(Calendar.YEAR)

    // Year selection state (e.g., currentYear, currentYear - 1, currentYear + 1)
    val availableYears = listOf(currentYear - 1, currentYear, currentYear + 1)
    var selectedYear by remember {
        val initialYear = selectedMonthKey.split("-").firstOrNull()?.toIntOrNull() ?: currentYear
        mutableIntStateOf(initialYear)
    }

    val monthsList = listOf(
        Pair(1, "January"), Pair(2, "February"), Pair(3, "March"),
        Pair(4, "April"), Pair(5, "May"), Pair(6, "June"),
        Pair(7, "July"), Pair(8, "August"), Pair(9, "September"),
        Pair(10, "October"), Pair(11, "November"), Pair(12, "December")
    )

    val currentSelectedAnalytics = last12Months.firstOrNull { it.monthKey == selectedMonthKey }
    val selectedTotalSpend = currentSelectedAnalytics?.totalSpent
        ?: currentMonthExpenses.sumOf { it.amount }
    val selectedSavings = monthlySalary - selectedTotalSpend

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calendar_analytics_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp)
    ) {
        // 1. Calendar Month & Year Selector Bento Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BentoCardBorder, RoundedCornerShape(24.dp))
                    .testTag("calendar_picker_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BentoLavenderContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Calendar",
                                        tint = BentoPurpleDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Permanent History",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BentoTextPrimary
                                )
                                Text(
                                    text = "Select any Year & Month to inspect spend & savings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BentoTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Year Selection Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { selectedYear -= 1 },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Year",
                                tint = BentoPurpleDark
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableYears.forEach { yr ->
                                FilterChip(
                                    selected = selectedYear == yr,
                                    onClick = { selectedYear = yr },
                                    label = {
                                        Text(
                                            text = yr.toString(),
                                            fontWeight = if (selectedYear == yr) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BentoPurpleDark,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = { selectedYear += 1 },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Year",
                                tint = BentoPurpleDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Month Grid (4 rows x 3 columns)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        monthsList.chunked(3).forEach { rowMonths ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowMonths.forEach { (mNum, mName) ->
                                    val key = String.format(Locale.US, "%04d-%02d", selectedYear, mNum)
                                    val isSelected = selectedMonthKey == key
                                    val shortName = mName.take(3)

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) BentoPurplePrimary else BentoLavenderCard,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSelectMonth(key) }
                                            .testTag("cal_month_$key")
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = shortName,
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) Color.White else BentoPurpleDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Selected Month Savings & Spend Summary Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoLavenderCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selected_month_summary_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ExpenseEntity.formatMonthDisplay(selectedMonthKey).uppercase(Locale.US),
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = BentoPurpleDark
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedSavings >= 0) SavioSavingsGreenBg else SavioSpendRedBg
                        ) {
                            Text(
                                text = if (selectedSavings >= 0) "SAVED IN $currency" else "OVERSPENT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedSavings >= 0) SavioSavingsGreen else SavioSpendRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Amount Saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoTextSecondary
                            )
                            Text(
                                text = if (selectedSavings >= 0) {
                                    "+$currency${numberFormatter.format(selectedSavings)}"
                                } else {
                                    "-$currency${numberFormatter.format(-selectedSavings)}"
                                },
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = if (selectedSavings >= 0) SavioSavingsGreen else SavioSpendRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Spent",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoTextSecondary
                            )
                            Text(
                                text = "$currency${numberFormatter.format(selectedTotalSpend)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = BentoDebitRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Salary vs Spend bar
                    val salaryPercent = if (monthlySalary > 0) {
                        ((selectedTotalSpend / monthlySalary) * 100).toInt()
                    } else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Monthly Salary: $currency${numberFormatter.format(monthlySalary)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoPurpleDark
                        )
                        Text(
                            text = "$salaryPercent% of salary spent",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedTotalSpend > monthlySalary) BentoDebitRed else BentoPurplePrimary
                        )
                    }
                }
            }
        }

        // 3. The 12-Month Spend vs Saved Analytics Vertical Bar Graph
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BentoCardBorder, RoundedCornerShape(24.dp))
                    .testTag("spend_savings_graph_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BentoLavenderContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Insights,
                                        contentDescription = "Graph",
                                        tint = BentoPurpleDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "12-Month Spend vs Saved Graph",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BentoTextPrimary
                                )
                                Text(
                                    text = "Upper red = Spent • Lower green = Saved",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BentoTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SavioSpendRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Upper: Amount Spent (Red)",
                                style = MaterialTheme.typography.labelSmall,
                                color = BentoTextPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SavioSavingsGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lower: Amount Saved (Green)",
                                style = MaterialTheme.typography.labelSmall,
                                color = BentoTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // The 12-Month Vertical Stacked Bar Chart
                    TwelveMonthStackedBarGraph(
                        months = last12Months,
                        selectedMonthKey = selectedMonthKey,
                        onSelectMonth = onSelectMonth,
                        currency = currency
                    )
                }
            }
        }

        // 4. Permanent Transactions in Selected Month
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Permanent Records (${currentMonthExpenses.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = BentoTextPrimary
                )
                Text(
                    text = ExpenseEntity.formatMonthDisplay(selectedMonthKey),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = BentoPurplePrimary
                )
            }
        }

        if (currentMonthExpenses.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BentoLavenderCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenditures recorded for ${ExpenseEntity.formatMonthDisplay(selectedMonthKey)}. New SMS will be auto-saved here permanently.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BentoTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = currentMonthExpenses,
                key = { it.id }
            ) { expense ->
                TransactionItemCard(
                    expense = expense,
                    currency = currency,
                    onDelete = {} // view-through in calendar
                )
            }
        }
    }
}

/**
 * 12-Month Vertical Stacked Bar Chart:
 * Each month has a vertical column:
 * - Upper section is RED (amount spent)
 * - Lower section is GREEN (amount saved)
 */
@Composable
fun TwelveMonthStackedBarGraph(
    months: List<MonthAnalytics>,
    selectedMonthKey: String,
    onSelectMonth: (String) -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Find the max value (either salary or max spent) to normalize height
    val maxBarValue = remember(months) {
        val maxSpend = months.maxOfOrNull { it.totalSpent } ?: 10000.0
        val maxSalary = months.maxOfOrNull { it.salary } ?: 50000.0
        maxOf(maxSpend, maxSalary, 10000.0)
    }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }
    }

    var inspectedMonth by remember { mutableStateOf<MonthAnalytics?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Active inspector chip if tapped
        inspectedMonth?.let { item ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BentoLavenderContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = item.monthLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = BentoPurpleDark
                        )
                        Text(
                            text = "Salary: $currency${numberFormat.format(item.salary)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BentoTextSecondary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Spent (Upper)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SavioSpendRed
                            )
                            Text(
                                text = "$currency${numberFormat.format(item.totalSpent)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SavioSpendRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Saved (Lower)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SavioSavingsGreen
                            )
                            Text(
                                text = "$currency${numberFormat.format(item.savedAmount.coerceAtLeast(0.0))}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SavioSavingsGreen
                            )
                        }
                    }
                }
            }
        }

        // Horizontal scroll container for the 12 month columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            months.forEach { monthData ->
                val isSelected = monthData.monthKey == selectedMonthKey

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            inspectedMonth = monthData
                            onSelectMonth(monthData.monthKey)
                        }
                        .testTag("bar_${monthData.monthKey}")
                ) {
                    // Vertical Bar (Height 160.dp)
                    // Stacked: Top is Red (Spent), Bottom is Green (Saved)
                    val chartHeightDp = 150.dp
                    val totalNorm = (monthData.salary / maxBarValue).coerceIn(0.1, 1.0)
                    val spentRatio = if (monthData.salary > 0) {
                        (monthData.totalSpent / monthData.salary).coerceIn(0.0, 1.0)
                    } else 1.0
                    val savedRatio = (1.0 - spentRatio).coerceAtLeast(0.0)

                    val barWidth = if (isSelected) 30.dp else 24.dp

                    Box(
                        modifier = Modifier
                            .height(chartHeightDp)
                            .width(barWidth),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Background track
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(barWidth)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BentoLavenderContainer.copy(alpha = 0.5f))
                        )

                        // The active stacked column representing the salary/expenditure
                        Column(
                            modifier = Modifier
                                .fillMaxHeight(totalNorm.toFloat())
                                .width(barWidth)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isSelected) Modifier.border(2.dp, BentoPurpleDark, RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                        ) {
                            // UPPER PART: RED for amount spent
                            Box(
                                modifier = Modifier
                                    .weight((spentRatio.toFloat()).coerceAtLeast(0.05f))
                                    .fillMaxWidth()
                                    .background(SavioSpendRed)
                            )

                            // LOWER PART: GREEN for amount saved
                            if (savedRatio > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(savedRatio.toFloat())
                                        .fillMaxWidth()
                                        .background(SavioSavingsGreen)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Month Short Label (e.g. "Sep", "Aug")
                    Text(
                        text = monthData.shortLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = if (isSelected) BentoPurpleDark else BentoTextSecondary
                    )
                }
            }
        }
    }
}
