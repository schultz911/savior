package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.ExpenseType
import com.example.ui.ExpenseFilter
import com.example.ui.theme.SavioTransferIndigo
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.theme.SavioSpendRed
import com.example.ui.theme.SavioSpendRedBg
import com.example.ui.theme.SavioSpendRose
import com.example.ui.theme.SavioSpendRoseBg
import com.example.ui.models.DailyBurnDownData
import com.example.ui.models.InstrumentSpendSummary
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
    allExpenses: List<ExpenseEntity> = emptyList(),
    burnDownData: DailyBurnDownData? = null,
    instruments: List<InstrumentSpendSummary> = emptyList(),
    selectedAccount: String? = null,
    onSelectAccount: (String?) -> Unit = {},
    onSelectMonth: (String) -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }

    var selectedDrilldownMonthKey by remember { mutableStateOf<String?>(null) }
    var analyticsFilter by remember { mutableStateOf(ExpenseFilter.ALL) }

    val currentCal = remember { Calendar.getInstance() }
    val currentYear = currentCal.get(Calendar.YEAR)

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

    val drilldownExpenses = remember(selectedDrilldownMonthKey, allExpenses, currentMonthExpenses, selectedMonthKey) {
        val key = selectedDrilldownMonthKey ?: return@remember emptyList<ExpenseEntity>()
        if (allExpenses.isNotEmpty()) {
            allExpenses.filter { it.monthKey == key }
        } else if (key == selectedMonthKey) {
            currentMonthExpenses
        } else {
            emptyList()
        }
    }

    val displayedExpenses = remember(currentMonthExpenses, analyticsFilter, selectedAccount) {
        currentMonthExpenses.filter { item ->
            val matchesFilter = when (analyticsFilter) {
                ExpenseFilter.ALL -> true
                ExpenseFilter.SPENDS -> item.type == ExpenseType.MERCHANT &&
                        !item.category.equals("Self", ignoreCase = true) &&
                        !item.category.equals("Credit Card Bill", ignoreCase = true)
                ExpenseFilter.TRANSFERS -> item.type == ExpenseType.P2P &&
                        !item.category.equals("Self", ignoreCase = true) &&
                        !item.category.equals("Credit Card Bill", ignoreCase = true)
                ExpenseFilter.CREDIT_CARDS -> item.type == ExpenseType.CREDIT_CARD ||
                        item.category.equals("Credit Card Bill", ignoreCase = true)
                ExpenseFilter.SELF -> item.type == ExpenseType.SELF ||
                        item.category.equals("Self", ignoreCase = true)
            }
            val matchesAccount = selectedAccount == null || item.accountInfo.contains(selectedAccount, ignoreCase = true)
            matchesFilter && matchesAccount
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("analytics_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Calendar History & Month Selector Glass Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(24.dp))
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
                                color = SavioEmeraldContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SavioEmeraldBorder),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Calendar",
                                        tint = SavioEmerald,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Permanent History",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SavioSlateDark
                                )
                                Text(
                                    text = "Select any Year & Month to inspect spend and savings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SavioSlateMuted
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
                                tint = SavioSlateDark
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableYears.forEach { yr ->
                                val isSel = selectedYear == yr
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedYear = yr },
                                    label = {
                                        Text(
                                            text = yr.toString(),
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SavioEmerald,
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
                                tint = SavioSlateDark
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
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) SavioEmerald else GlassBackground,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) SavioEmerald else GlassCardBorder
                                        ),
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
                                                color = if (isSelected) Color.White else SavioSlateDark
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
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
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
                            color = SavioSlateDark
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Net Amount Saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = SavioSlateMuted
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
                                color = SavioSlateMuted
                            )
                            Text(
                                text = "$currency${numberFormatter.format(selectedTotalSpend)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = SavioSpendRose
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                            color = SavioSlateMuted
                        )
                        Text(
                            text = "$salaryPercent% of salary spent",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedTotalSpend > monthlySalary) SavioSpendRed else SavioEmerald
                        )
                    }
                }
            }
        }

        // Intra-Month Daily Spending Velocity & Burn-Down Curve
        if (burnDownData != null) {
            item {
                DailyBurnDownChart(
                    burnDownData = burnDownData,
                    currency = currency
                )
            }
        }

        // Multi-Account & Instrument Liquidity Intelligence
        if (instruments.isNotEmpty()) {
            item {
                InstrumentLiquidityCard(
                    instruments = instruments,
                    selectedAccount = selectedAccount,
                    onSelectAccount = onSelectAccount,
                    currency = currency
                )
            }
        }

        // 3. The 12-Month Spend vs Saved Analytics Vertical Bar Graph (Fixed scaling)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(24.dp))
                    .testTag("spend_savings_graph_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val drilldownMonth = selectedDrilldownMonthKey
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SavioEmeraldContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SavioEmeraldBorder),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Insights,
                                        contentDescription = "Analytics",
                                        tint = SavioEmerald,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (drilldownMonth != null) "Category-Wise Spends" else "12-Month Spend Analytics",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SavioSlateDark
                                )
                                Text(
                                    text = if (drilldownMonth != null) "Spending breakdown for ${ExpenseEntity.formatMonthDisplay(drilldownMonth)}" else "Dynamic vertical scale across all 12 months",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SavioSlateMuted
                                )
                            }
                        }

                        if (drilldownMonth != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SavioEmeraldContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SavioEmeraldBorder),
                                modifier = Modifier
                                    .clickable { selectedDrilldownMonthKey = null }
                                    .testTag("month_drilldown_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${ExpenseEntity.formatMonthDisplay(drilldownMonth)} ✕",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SavioEmerald
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (drilldownMonth == null) {
                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(SavioSpendRose)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Spent (Red)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SavioSlateDark
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(SavioSavingsGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Saved (Green)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SavioSlateDark
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "┄┄",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = SavioEmerald
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Salary (Dotted Line)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SavioEmerald
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // The 12-Month Vertical Stacked Bar Chart with Full-Height Scaling
                        TwelveMonthStackedBarGraph(
                            months = last12Months,
                            selectedMonthKey = selectedMonthKey,
                            onSelectMonth = onSelectMonth,
                            onBarSelect = { barMonthKey ->
                                selectedDrilldownMonthKey = barMonthKey
                            },
                            currency = currency
                        )
                    } else {
                        // Drilldown: Category-wise Bar Graph for selected month
                        CategoryWiseBarGraph(
                            monthKey = drilldownMonth,
                            expenses = drilldownExpenses,
                            currency = currency
                        )
                    }
                }
            }
        }

        // 4. Transactions List Header with Filter Chips (Replaces Open Live Dashboard link)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "Transactions in ${ExpenseEntity.formatMonthDisplay(selectedMonthKey)} (${displayedExpenses.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SavioSlateDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filters below header: All, Spends, Transfers, Credit Cards, Self
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseFilter.entries.forEach { filter ->
                        val isSelected = analyticsFilter == filter
                        val filterColor = when (filter) {
                            ExpenseFilter.ALL -> SavioEmerald
                            ExpenseFilter.SPENDS -> SavioSpendRose
                            ExpenseFilter.TRANSFERS -> SavioTransferIndigo
                            ExpenseFilter.CREDIT_CARDS -> Color(0xFF7C3AED)
                            ExpenseFilter.SELF -> SavioEmerald
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { analyticsFilter = filter },
                            label = {
                                Text(
                                    text = filter.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = filterColor.copy(alpha = 0.15f),
                                selectedLabelColor = filterColor
                            ),
                            modifier = Modifier.testTag("analytics_filter_${filter.name}")
                        )
                    }
                }
            }
        }

        if (displayedExpenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder)
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenditures found for ${analyticsFilter.label} in ${ExpenseEntity.formatMonthDisplay(selectedMonthKey)}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SavioSlateMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = displayedExpenses,
                key = { it.id }
            ) { expense ->
                TransactionItemCard(
                    expense = expense,
                    currency = currency,
                    onDelete = {} // view-through in analytics
                )
            }
        }
    }
}

/**
 * 12-Month Vertical Stacked Bar Chart with Proper Full-Height Scaling:
 * Normalizes all month columns against the maximum monthly spend/salary,
 * ensuring bars dynamically scale across the entire height of the chart!
 */
@Composable
fun TwelveMonthStackedBarGraph(
    months: List<MonthAnalytics>,
    selectedMonthKey: String,
    onSelectMonth: (String) -> Unit,
    currency: String,
    onBarSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Automatically scroll to the rightmost month (latest month) by default
    LaunchedEffect(months) {
        if (months.isNotEmpty()) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // Find the true maximum value across all 12 months to scale the chart height dynamically
    val maxChartVal = remember(months) {
        val maxSpend = months.maxOfOrNull { it.totalSpent } ?: 0.0
        val maxSalary = months.maxOfOrNull { it.salary } ?: 0.0
        maxOf(maxSpend, maxSalary, 1000.0)
    }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }
    }

    val currentSalary = remember(months) {
        months.map { it.salary }.firstOrNull { it > 0.0 } ?: 50000.0
    }
    val salaryFraction = remember(currentSalary, maxChartVal) {
        (currentSalary / maxChartVal).toFloat().coerceIn(0.08f, 0.95f)
    }

    var inspectedMonth by remember { mutableStateOf<MonthAnalytics?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Active inspector banner when user taps any column
        inspectedMonth?.let { item ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SavioEmeraldContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, SavioEmeraldBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = item.monthLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = SavioSlateDark
                        )
                        Text(
                            text = "Salary: $currency${numberFormat.format(item.salary)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SavioSlateMuted
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = SavioSpendRose
                            )
                            Text(
                                text = "$currency${numberFormat.format(item.totalSpent)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SavioSpendRose
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Saved",
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

        // Horizontal scroll container for the 12 month columns with dotted salary line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 8.dp)
                .drawWithContent {
                    drawContent()
                    // Dotted line across the entire graph representing the monthly salary
                    val chartHeightPx = 160.dp.toPx()
                    val labelHeightPx = 8.dp.toPx() + 18.dp.toPx()
                    val barBottomY = size.height - labelHeightPx
                    val salaryLineY = barBottomY - (salaryFraction * chartHeightPx)

                    drawLine(
                        color = SavioEmerald,
                        start = Offset(0f, salaryLineY),
                        end = Offset(size.width, salaryLineY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                },
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
                            onBarSelect(monthData.monthKey)
                        }
                        .testTag("bar_${monthData.monthKey}")
                ) {
                    val chartHeightDp = 160.dp
                    val barWidth = if (isSelected) 32.dp else 26.dp

                    // Calculate accurate scaling across the full height
                    val monthSpend = monthData.totalSpent
                    val monthSalary = monthData.salary
                    val monthMax = maxOf(monthSpend, monthSalary)

                    // Height fraction of this month's activity relative to max across 12 months (0.10f floor to 1.0f max)
                    val columnHeightFraction = (monthMax / maxChartVal).toFloat().coerceIn(0.12f, 1.0f)

                    // Proportion of spent vs saved inside this month's column
                    val spentRatio = if (monthMax > 0) {
                        (monthSpend / monthMax).toFloat().coerceIn(0.04f, 1.0f)
                    } else 0.04f

                    val savedRatio = (1.0f - spentRatio).coerceAtLeast(0f)

                    Box(
                        modifier = Modifier
                            .height(chartHeightDp)
                            .width(barWidth),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Background track for full height
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(barWidth)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassBackground)
                                .border(1.dp, GlassCardBorder, RoundedCornerShape(8.dp))
                        )

                        // Active Stacked Bar scaling across the height
                        Column(
                            modifier = Modifier
                                .fillMaxHeight(columnHeightFraction)
                                .width(barWidth)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isSelected) Modifier.border(2.dp, SavioEmerald, RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                        ) {
                            // Upper part: RED for amount spent
                            Box(
                                modifier = Modifier
                                    .weight(spentRatio)
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(SavioSpendRose, SavioSpendRose.copy(alpha = 0.85f))
                                        )
                                    )
                            )

                            // Lower part: GREEN for amount saved
                            if (savedRatio > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .weight(savedRatio)
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(SavioSavingsGreen.copy(alpha = 0.85f), SavioSavingsGreen)
                                            )
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Month Short Label
                    Text(
                        text = monthData.shortLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = if (isSelected) SavioEmerald else SavioSlateMuted
                    )
                }
            }
        }
    }
}

data class CategorySpendBarItem(
    val category: String,
    val amount: Double,
    val count: Int
)

@Composable
fun CategoryWiseBarGraph(
    monthKey: String,
    expenses: List<ExpenseEntity>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }

    val categoryList = remember(expenses) {
        val nonExcluded = expenses.filter {
            !it.category.equals("Self", ignoreCase = true) &&
            !it.category.equals("Credit Card Bill", ignoreCase = true)
        }
        val targetList = if (nonExcluded.isNotEmpty()) nonExcluded else expenses
        targetList.groupBy { it.category.ifBlank { "General Spend" } }
            .map { (cat, list) ->
                CategorySpendBarItem(
                    category = cat,
                    amount = list.sumOf { it.amount },
                    count = list.size
                )
            }
            .sortedByDescending { it.amount }
    }

    val totalMonthSpend = remember(categoryList) {
        categoryList.sumOf { it.amount }
    }

    val maxCategoryAmount = remember(categoryList) {
        categoryList.maxOfOrNull { it.amount } ?: 1.0
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (categoryList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No category spends recorded for ${ExpenseEntity.formatMonthDisplay(monthKey)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SavioSlateMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            categoryList.forEach { item ->
                val fraction = if (maxCategoryAmount > 0) {
                    (item.amount / maxCategoryAmount).toFloat().coerceIn(0.06f, 1.0f)
                } else 0.06f
                val pct = if (totalMonthSpend > 0) {
                    ((item.amount / totalMonthSpend) * 100).toInt()
                } else 0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SavioEmeraldContainer,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = getAnalyticsCategoryIcon(item.category),
                                        contentDescription = item.category,
                                        tint = SavioEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SavioSlateDark
                                )
                                Text(
                                    text = "${item.count} ${if (item.count == 1) "txn" else "txns"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SavioSlateMuted
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$currency${numberFormatter.format(item.amount)}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = SavioSpendRose
                            )
                            Text(
                                text = "$pct% of month",
                                style = MaterialTheme.typography.labelSmall,
                                color = SavioSlateMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal bar graph
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(GlassBackground)
                            .border(0.5.dp, GlassCardBorder, RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SavioEmerald, SavioEmerald.copy(alpha = 0.75f))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun getAnalyticsCategoryIcon(category: String): ImageVector {
    val lower = category.lowercase()
    return when {
        lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") -> Icons.Default.Fastfood
        lower.contains("grocer") -> Icons.Default.ShoppingCart
        lower.contains("shop") || lower.contains("ecommerce") -> Icons.Default.ShoppingBag
        lower.contains("travel") || lower.contains("flight") || lower.contains("commute") -> Icons.Default.Flight
        lower.contains("bill") || lower.contains("utility") -> Icons.Default.Receipt
        lower.contains("transfer") -> Icons.Default.SwapHoriz
        lower.contains("credit card") -> Icons.Default.CreditCard
        lower.contains("self") || lower.contains("invest") || lower.contains("bank") -> Icons.Default.AccountBalance
        else -> Icons.Default.Insights
    }
}

