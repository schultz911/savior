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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.DeleteOutline
import com.example.ui.models.InstrumentSpendSummary
import java.text.NumberFormat
import java.text.SimpleDateFormat
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
    onDeleteMonth: ((String) -> Unit)? = null,
    onEditMerchant: ((id: Long, oldMerchant: String, newMerchant: String, category: String) -> Unit)? = null,
    onDeleteExpense: ((Long) -> Unit)? = null,
    onAssignCategory: ((ExpenseEntity) -> Unit)? = null,
    onToggleBlacklist: ((String) -> Unit)? = null,
    onToggleExclude: ((Long, Boolean) -> Unit)? = null,
    onToggleRecurring: ((Long, Boolean) -> Unit)? = null,
    isBlacklistedMerchant: ((String) -> Boolean)? = null,
    onUpdateRefundSettlement: ((Long, Double) -> Unit)? = null,
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

    val availableYears = remember(allExpenses, currentYear) {
        val yearsFromData = allExpenses.mapNotNull { it.monthKey.split("-").firstOrNull()?.toIntOrNull() }
        (yearsFromData + listOf(currentYear - 1, currentYear, currentYear + 1)).distinct().sorted()
    }
    var selectedYear by remember {
        val initialYear = selectedMonthKey.split("-").firstOrNull()?.toIntOrNull() ?: currentYear
        mutableIntStateOf(initialYear)
    }

    val selectedMonthExpenses = remember(selectedMonthKey, allExpenses, currentMonthExpenses) {
        if (allExpenses.isNotEmpty()) {
            allExpenses.filter { it.monthKey == selectedMonthKey }
        } else {
            currentMonthExpenses
        }
    }

    val monthsForYear = remember(selectedYear, allExpenses, currentMonthExpenses, monthlySalary, last12Months) {
        val sdfShort = SimpleDateFormat("MMM", Locale.US)
        val sdfFull = SimpleDateFormat("MMM yyyy", Locale.US)
        val cal = Calendar.getInstance()
        val expensesByMonth = if (allExpenses.isNotEmpty()) allExpenses.groupBy { it.monthKey } else emptyMap()
        (1..12).mapNotNull { monthNum ->
            cal.set(selectedYear, monthNum - 1, 1)
            val monthKey = String.format(Locale.US, "%04d-%02d", selectedYear, monthNum)
            val fullLabel = sdfFull.format(cal.time)
            val shortLabel = sdfShort.format(cal.time)

            val expensesForM = expensesByMonth[monthKey] ?: if (monthKey == selectedMonthKey) {
                currentMonthExpenses
            } else emptyList()

            val cached = last12Months.firstOrNull { it.monthKey == monthKey }
            val hasData = expensesForM.isNotEmpty() || (cached != null && cached.totalSpent > 0.0)

            if (!hasData) {
                null
            } else {
                val totalSpent = if (expensesForM.isNotEmpty()) {
                    expensesForM.filter {
                        !it.isExcluded &&
                        !it.category.equals("Self", ignoreCase = true) &&
                        !it.category.equals("Credit Card Bill", ignoreCase = true)
                    }.sumOf { it.effectiveSpendAmount }.coerceAtLeast(0.0)
                } else {
                    cached?.totalSpent ?: 0.0
                }
                val savedAmount = monthlySalary - totalSpent
                val isOverspent = totalSpent > monthlySalary
                val rate = if (monthlySalary > 0) (savedAmount / monthlySalary) * 100.0 else 0.0

                MonthAnalytics(
                    monthKey = monthKey,
                    monthLabel = fullLabel,
                    shortLabel = shortLabel,
                    year = selectedYear,
                    monthNumber = monthNum,
                    totalSpent = totalSpent,
                    salary = monthlySalary,
                    savedAmount = savedAmount,
                    isOverspent = isOverspent,
                    savingsRate = rate
                )
            }
        }
    }

    val currentSelectedAnalytics = remember(selectedMonthKey, monthsForYear) {
        monthsForYear.firstOrNull { it.monthKey == selectedMonthKey }
    }

    val selectedTotalSpend = remember(selectedMonthExpenses, currentSelectedAnalytics) {
        currentSelectedAnalytics?.totalSpent
            ?: selectedMonthExpenses.filter {
                !it.isExcluded &&
                !it.category.equals("Self", ignoreCase = true) &&
                !it.category.equals("Credit Card Bill", ignoreCase = true)
            }.sumOf { it.effectiveSpendAmount }.coerceAtLeast(0.0)
    }
    val selectedSavings = monthlySalary - selectedTotalSpend

    // Month-over-Month (MoM) Delta Calculations
    val prevMonthKey = remember(selectedMonthKey) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdf.parse(selectedMonthKey)
            if (date != null) {
                val c = Calendar.getInstance().apply { time = date }
                c.add(Calendar.MONTH, -1)
                sdf.format(c.time)
            } else null
        } catch (e: Exception) { null }
    }

    val prevMonthShortLabel = remember(prevMonthKey) {
        if (prevMonthKey != null) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
                val sdfShort = SimpleDateFormat("MMM", Locale.US)
                val d = sdf.parse(prevMonthKey)
                if (d != null) sdfShort.format(d) else "last month"
            } catch (e: Exception) { "last month" }
        } else "last month"
    }

    val prevMonthExpenses = remember(prevMonthKey, allExpenses) {
        if (prevMonthKey != null && allExpenses.isNotEmpty()) {
            allExpenses.filter { it.monthKey == prevMonthKey }
        } else emptyList()
    }

    val prevTotalSpend = remember(prevMonthExpenses, last12Months, prevMonthKey) {
        if (prevMonthExpenses.isNotEmpty()) {
            prevMonthExpenses.filter {
                !it.isExcluded &&
                !it.category.equals("Self", ignoreCase = true) &&
                !it.category.equals("Credit Card Bill", ignoreCase = true)
            }.sumOf { it.effectiveSpendAmount }.coerceAtLeast(0.0)
        } else {
            last12Months.firstOrNull { it.monthKey == prevMonthKey }?.totalSpent ?: 0.0
        }
    }

    val momDeltaSpend = selectedTotalSpend - prevTotalSpend
    val momDeltaPercent = if (prevTotalSpend > 0) {
        ((selectedTotalSpend - prevTotalSpend) / prevTotalSpend) * 100.0
    } else null

    val displayedExpenses = remember(selectedMonthExpenses, analyticsFilter, selectedAccount) {
        selectedMonthExpenses.filter { item ->
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
        // 1. Top Card: Permanent History & 12-Month Spend Analytics Merged
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = "Analytics",
                                tint = SavioEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "12-Month Spend Analytics",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SavioSlateDark
                                )
                                Text(
                                    text = if (monthsForYear.isNotEmpty()) {
                                        "Permanent history · ${monthsForYear.size} active month(s) in $selectedYear"
                                    } else {
                                        "Permanent history · No data for $selectedYear"
                                    },
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
                            onClick = {
                                val targetYear = selectedYear - 1
                                selectedYear = targetYear
                                val monthPart = selectedMonthKey.split("-").getOrNull(1)?.toIntOrNull() ?: 1
                                val candidateKey = String.format(Locale.US, "%04d-%02d", targetYear, monthPart)
                                val yearMonthsWithData = allExpenses.filter { it.monthKey.startsWith("$targetYear-") }
                                    .map { it.monthKey }.distinct().sorted()
                                val keyToSelect = if (yearMonthsWithData.contains(candidateKey)) {
                                    candidateKey
                                } else {
                                    yearMonthsWithData.lastOrNull() ?: candidateKey
                                }
                                onSelectMonth(keyToSelect)
                            },
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
                                    onClick = {
                                        selectedYear = yr
                                        val monthPart = selectedMonthKey.split("-").getOrNull(1)?.toIntOrNull() ?: 1
                                        val candidateKey = String.format(Locale.US, "%04d-%02d", yr, monthPart)
                                        val yearMonthsWithData = allExpenses.filter { it.monthKey.startsWith("$yr-") }
                                            .map { it.monthKey }.distinct().sorted()
                                        val keyToSelect = if (yearMonthsWithData.contains(candidateKey)) {
                                            candidateKey
                                        } else {
                                            yearMonthsWithData.lastOrNull() ?: candidateKey
                                        }
                                        onSelectMonth(keyToSelect)
                                    },
                                    label = {
                                        Text(
                                            text = yr.toString(),
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.White,
                                        labelColor = SavioSlateDark,
                                        selectedContainerColor = SavioEmerald,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSel,
                                        borderColor = GlassCardBorder,
                                        selectedBorderColor = SavioEmerald
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val targetYear = selectedYear + 1
                                selectedYear = targetYear
                                val monthPart = selectedMonthKey.split("-").getOrNull(1)?.toIntOrNull() ?: 1
                                val candidateKey = String.format(Locale.US, "%04d-%02d", targetYear, monthPart)
                                val yearMonthsWithData = allExpenses.filter { it.monthKey.startsWith("$targetYear-") }
                                    .map { it.monthKey }.distinct().sorted()
                                val keyToSelect = if (yearMonthsWithData.contains(candidateKey)) {
                                    candidateKey
                                } else {
                                    yearMonthsWithData.lastOrNull() ?: candidateKey
                                }
                                onSelectMonth(keyToSelect)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Year",
                                tint = SavioSlateDark
                            )
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
                        months = monthsForYear,
                        selectedMonthKey = selectedMonthKey,
                        onSelectMonth = onSelectMonth,
                        onBarSelect = { onSelectMonth(it) },
                        onDeleteMonth = onDeleteMonth,
                        currency = currency
                    )
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
                            if (momDeltaPercent != null) {
                                val isLess = momDeltaSpend < 0
                                val deltaFormatted = "$currency${numberFormatter.format(kotlin.math.abs(momDeltaSpend))}"
                                val pctText = String.format(Locale.US, "%.1f", kotlin.math.abs(momDeltaPercent))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isLess) SavioSavingsGreenBg else SavioSpendRoseBg,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = if (isLess) "▼ $pctText% vs $prevMonthShortLabel (-$deltaFormatted)"
                                               else "▲ $pctText% vs $prevMonthShortLabel (+$deltaFormatted)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = if (isLess) SavioSavingsGreen else SavioSpendRose,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
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

        // 3. Intra-Month Daily Spending Velocity & Burn-Down Curve
        if (burnDownData != null) {
            item {
                DailyBurnDownChart(
                    burnDownData = burnDownData,
                    currency = currency
                )
            }
        }

        // 4. Category-Wise Spends Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(24.dp))
                    .testTag("category_wise_spends_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = "Category Breakdown",
                                tint = SavioEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Category-Wise Spends",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SavioSlateDark
                                )
                                Text(
                                    text = "Spending breakdown for ${ExpenseEntity.formatMonthDisplay(selectedMonthKey)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SavioSlateMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    CategoryWiseBarGraph(
                        monthKey = selectedMonthKey,
                        expenses = selectedMonthExpenses,
                        currency = currency,
                        prevMonthExpenses = prevMonthExpenses,
                        prevMonthShortLabel = prevMonthShortLabel
                    )
                }
            }
        }

        // 5. Multi-Account & Instrument Liquidity Intelligence
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
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = SavioSlateBody,
                                selectedContainerColor = filterColor.copy(alpha = 0.15f),
                                selectedLabelColor = filterColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = GlassCardBorder,
                                selectedBorderColor = filterColor.copy(alpha = 0.35f)
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
                    isBlacklisted = isBlacklistedMerchant?.invoke(expense.merchantOrRecipient) ?: false,
                    onDelete = { onDeleteExpense?.invoke(it) },
                    onAssignCategory = onAssignCategory,
                    onToggleBlacklist = onToggleBlacklist,
                    onToggleExclude = onToggleExclude,
                    onToggleRecurring = onToggleRecurring,
                    onEditMerchant = onEditMerchant,
                    onUpdateRefundSettlement = onUpdateRefundSettlement
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TwelveMonthStackedBarGraph(
    months: List<MonthAnalytics>,
    selectedMonthKey: String,
    onSelectMonth: (String) -> Unit,
    currency: String,
    onBarSelect: (String) -> Unit = {},
    onDeleteMonth: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    var monthPendingDeletion by remember { mutableStateOf<MonthAnalytics?>(null) }

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

    Column(modifier = modifier.fillMaxWidth()) {
        if (months.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = GlassBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        tint = SavioSlateMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No spend data available for this year",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = SavioSlateMuted
                    )
                }
            }
        } else {
            // Month columns with dotted salary line
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = if (months.size <= 5) Alignment.Center else Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .then(if (months.size > 5) Modifier.horizontalScroll(scrollState) else Modifier)
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
                    horizontalArrangement = if (months.size <= 5) Arrangement.spacedBy(20.dp) else Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    months.forEach { monthData ->
                        val isSelected = monthData.monthKey == selectedMonthKey

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        onSelectMonth(monthData.monthKey)
                                        onBarSelect(monthData.monthKey)
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        monthPendingDeletion = monthData
                                    }
                                )
                                .testTag("bar_${monthData.monthKey}")
                        ) {
                            val chartHeightDp = 160.dp
                            val barWidth = if (months.size <= 4) 44.dp else if (isSelected) 32.dp else 26.dp

                            // Calculate accurate scaling across the full height
                            val monthSpend = monthData.totalSpent
                            val monthSalary = monthData.salary
                            val monthMax = maxOf(monthSpend, monthSalary)

                            // Height fraction of this month's activity relative to max across months (0.12f floor to 1.0f max)
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

        if (monthPendingDeletion != null) {
            val targetMonth = monthPendingDeletion!!
            AlertDialog(
                onDismissRequest = { monthPendingDeletion = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = SavioSpendRose,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Delete Data for ${targetMonth.monthLabel}?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Are you sure you want to permanently delete all recorded expenses and transaction data for ${targetMonth.monthLabel}?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SavioSlateBody
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SavioSpendRoseBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Total Spend: $currency${numberFormat.format(targetMonth.totalSpent)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = SavioSpendRose
                                )
                                Text(
                                    text = "This action cannot be undone.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SavioSpendRose
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val keyToDelete = targetMonth.monthKey
                            monthPendingDeletion = null
                            onDeleteMonth?.invoke(keyToDelete)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SavioSpendRose)
                    ) {
                        Text("Delete Month", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { monthPendingDeletion = null }) {
                        Text("Cancel", color = SavioSlateDark)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
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
    modifier: Modifier = Modifier,
    prevMonthExpenses: List<ExpenseEntity> = emptyList(),
    prevMonthShortLabel: String = ""
) {
    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }

    val prevCategoryTotals = remember(prevMonthExpenses) {
        val nonExcluded = prevMonthExpenses.filter {
            !it.isExcluded &&
            !it.category.equals("Self", ignoreCase = true) &&
            !it.category.equals("Credit Card Bill", ignoreCase = true)
        }
        val targetList = if (nonExcluded.isNotEmpty()) nonExcluded else prevMonthExpenses
        targetList.groupBy { it.category.ifBlank { "General Spend" } }
            .mapValues { (_, list) -> list.sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0.0) } }
    }

    val categoryList = remember(expenses) {
        val nonExcluded = expenses.filter {
            !it.isExcluded &&
            !it.category.equals("Self", ignoreCase = true) &&
            !it.category.equals("Credit Card Bill", ignoreCase = true)
        }
        val targetList = if (nonExcluded.isNotEmpty()) nonExcluded else expenses
        targetList.groupBy { it.category.ifBlank { "General Spend" } }
            .map { (cat, list) ->
                CategorySpendBarItem(
                    category = cat,
                    amount = list.sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0.0) },
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$pct% of month",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SavioSlateMuted
                                )
                                if (prevMonthExpenses.isNotEmpty()) {
                                    val prevCatSpend = prevCategoryTotals[item.category]
                                    if (prevCatSpend == null || prevCatSpend == 0.0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "• ★ New",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                            color = SavioTransferIndigo
                                        )
                                    } else {
                                        val delta = item.amount - prevCatSpend
                                        val deltaFormatted = "$currency${numberFormatter.format(kotlin.math.abs(delta))}"
                                        val isDrop = delta < 0
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isDrop) "• ▼ -$deltaFormatted" else "• ▲ +$deltaFormatted",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                            color = if (isDrop) SavioSavingsGreen else SavioSpendRose
                                        )
                                    }
                                }
                            }
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

