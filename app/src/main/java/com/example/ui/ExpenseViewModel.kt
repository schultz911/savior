package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SpendTrackerApplication
import com.example.data.ExpenseEntity
import com.example.data.ExpensePreferences
import com.example.data.ExpenseRepository
import com.example.data.ExpenseType
import com.example.service.LiveExpenditureNotificationService
import com.example.sms.SampleSmsData
import com.example.ui.models.MonthAnalytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import com.example.engine.PredictedRecurringBill
import com.example.engine.RecurringDetectionEngine
import com.example.data.MerchantRuleEntity
import com.example.ui.models.DailyBurnDownData
import com.example.ui.models.DaySpendPoint
import com.example.ui.models.InstrumentSpendSummary
import com.example.ui.models.InstrumentType

enum class ExpenseFilter(val label: String) {
    ALL("All"),
    SPENDS("Spends"),
    TRANSFERS("Transfers"),
    CREDIT_CARDS("Credit Cards"),
    SELF("Self")
}

enum class AmountRange(val label: String) {
    ALL("All"),
    UNDER_500("< ₹500"),
    MID_500_2000("₹500 - ₹2K"),
    OVER_2000("> ₹2K")
}

enum class PacingStatus(val label: String) {
    ON_TRACK("On Track"),
    CAUTION("Caution"),
    OVER_PACED("Over-Paced")
}

data class SafeSpendPacing(
    val safeDailySpend: Double = 0.0,
    val todaySpent: Double = 0.0,
    val daysRemaining: Int = 1,
    val daysInMonth: Int = 30,
    val currentDay: Int = 1,
    val status: PacingStatus = PacingStatus.ON_TRACK,
    val remainingDiscretionary: Double = 0.0,
    val upcomingRecurringTotal: Double = 0.0
)

data class FilterCriteria(
    val filter: ExpenseFilter = ExpenseFilter.ALL,
    val query: String = "",
    val categoryFilter: String? = null,
    val amountRange: AmountRange = AmountRange.ALL,
    val onlyRecurring: Boolean = false,
    val accountFilter: String? = null
)

enum class SavioScreenTab {
    DASHBOARD,
    ANALYTICS,
    SETTINGS
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository,
    private val preferences: ExpensePreferences
) : AndroidViewModel(application) {

    private val currentDefaultMonth = ExpenseEntity.formatMonthKey(System.currentTimeMillis())

    private val _currentTab = MutableStateFlow(SavioScreenTab.DASHBOARD)
    val currentTab: StateFlow<SavioScreenTab> = _currentTab.asStateFlow()

    private val _selectedMonthKey = MutableStateFlow(currentDefaultMonth)
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ExpenseFilter.ALL)
    val selectedFilter: StateFlow<ExpenseFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedAmountRange = MutableStateFlow(AmountRange.ALL)
    val selectedAmountRange: StateFlow<AmountRange> = _selectedAmountRange.asStateFlow()

    private val _onlyRecurringFilter = MutableStateFlow(false)
    val onlyRecurringFilter: StateFlow<Boolean> = _onlyRecurringFilter.asStateFlow()

    private val _selectedAccountFilter = MutableStateFlow<String?>(null)
    val selectedAccountFilter: StateFlow<String?> = _selectedAccountFilter.asStateFlow()

    private val _isGlobalSearch = MutableStateFlow(false)
    val isGlobalSearch: StateFlow<Boolean> = _isGlobalSearch.asStateFlow()

    private val _isVelocityAlertsEnabled = MutableStateFlow(preferences.isVelocityAlertsEnabled)
    val isVelocityAlertsEnabled: StateFlow<Boolean> = _isVelocityAlertsEnabled.asStateFlow()

    private val _isAnomalyAlertsEnabled = MutableStateFlow(preferences.isAnomalyAlertsEnabled)
    val isAnomalyAlertsEnabled: StateFlow<Boolean> = _isAnomalyAlertsEnabled.asStateFlow()

    private val _isSearchExpanded = MutableStateFlow(false)
    val isSearchExpanded: StateFlow<Boolean> = _isSearchExpanded.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncFeedback = MutableStateFlow<String?>(null)
    val syncFeedback: StateFlow<String?> = _syncFeedback.asStateFlow()

    private val _currency = MutableStateFlow(preferences.currency)
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _monthlySalary = MutableStateFlow(
        if (preferences.monthlySalary > 0) preferences.monthlySalary else 50000.0
    )
    val monthlySalary: StateFlow<Double> = _monthlySalary.asStateFlow()

    private val _savingsGoal = MutableStateFlow(
        if (preferences.savingsGoal > 0) preferences.savingsGoal else 15000.0
    )
    val savingsGoal: StateFlow<Double> = _savingsGoal.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(preferences.monthlyBudget)
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _isPersistentNotificationEnabled =
        MutableStateFlow(preferences.isPersistentNotificationEnabled)
    val isPersistentNotificationEnabled: StateFlow<Boolean> =
        _isPersistentNotificationEnabled.asStateFlow()

    private val _openRouterApiKey = MutableStateFlow(preferences.openRouterApiKey)
    val openRouterApiKey: StateFlow<String> = _openRouterApiKey.asStateFlow()

    private val _categoryLimits = MutableStateFlow(preferences.getAllCategoryLimits())
    val categoryLimits: StateFlow<Map<String, Double>> = _categoryLimits.asStateFlow()

    // Blacklisted merchants set
    private val _blacklistedMerchants = MutableStateFlow(preferences.getBlacklistedMerchants())
    val blacklistedMerchants: StateFlow<Set<String>> = _blacklistedMerchants.asStateFlow()

    // Ignored recurring radar merchants set
    private val _ignoredRecurringMerchants = MutableStateFlow(preferences.getIgnoredRecurringMerchants())
    val ignoredRecurringMerchants: StateFlow<Set<String>> = _ignoredRecurringMerchants.asStateFlow()

    private val _isBiometricLockEnabled = MutableStateFlow(preferences.isBiometricLockEnabled)
    val isBiometricLockEnabled: StateFlow<Boolean> = _isBiometricLockEnabled.asStateFlow()

    private val _isPrivacyShieldEnabled = MutableStateFlow(preferences.isPrivacyShieldEnabled)
    val isPrivacyShieldEnabled: StateFlow<Boolean> = _isPrivacyShieldEnabled.asStateFlow()

    private val _lockTimeoutSeconds = MutableStateFlow(preferences.lockTimeoutSeconds)
    val lockTimeoutSeconds: StateFlow<Int> = _lockTimeoutSeconds.asStateFlow()

    fun isSelf(item: ExpenseEntity): Boolean =
        item.type == ExpenseType.SELF || item.category.equals("Self", ignoreCase = true)

    fun isCreditCard(item: ExpenseEntity): Boolean =
        item.type == ExpenseType.CREDIT_CARD || item.category.equals("Credit Card Bill", ignoreCase = true)

    fun isTransfer(item: ExpenseEntity): Boolean =
        item.type == ExpenseType.P2P && !isSelf(item) && !isCreditCard(item)

    fun isMerchantSpend(item: ExpenseEntity): Boolean =
        !isTransfer(item) && !isSelf(item) && !isCreditCard(item)

    val allMonthKeys: StateFlow<List<String>> = repository.allMonthKeys
        .map { keys ->
            if (!keys.contains(currentDefaultMonth)) {
                (listOf(currentDefaultMonth) + keys).distinct()
            } else {
                keys
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(currentDefaultMonth)
        )

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentMonthExpenses: StateFlow<List<ExpenseEntity>> = _selectedMonthKey
        .flatMapLatest { monthKey ->
            repository.getExpensesForMonth(monthKey)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val filterCriteria = combine(
        combine(_selectedFilter, _searchQuery, _selectedCategoryFilter) { f, q, c -> Triple(f, q, c) },
        combine(_selectedAmountRange, _onlyRecurringFilter, _selectedAccountFilter) { a, r, acc -> Triple(a, r, acc) }
    ) { (f, q, c), (a, r, acc) ->
        FilterCriteria(f, q, c, a, r, acc)
    }

    val filteredExpenses: StateFlow<List<ExpenseEntity>> = combine(
        combine(_isGlobalSearch, currentMonthExpenses, allExpenses) { isGlobal, monthExp, allExp ->
            if (isGlobal) allExp else monthExp
        },
        filterCriteria
    ) { expenses, criteria ->
        expenses.filter { item ->
            val matchesFilter = when (criteria.filter) {
                ExpenseFilter.ALL -> true
                ExpenseFilter.SPENDS -> isMerchantSpend(item)
                ExpenseFilter.TRANSFERS -> isTransfer(item)
                ExpenseFilter.CREDIT_CARDS -> isCreditCard(item)
                ExpenseFilter.SELF -> isSelf(item)
            }
            val matchesQuery = if (criteria.query.isBlank()) true else {
                item.merchantOrRecipient.contains(criteria.query, ignoreCase = true) ||
                        item.accountInfo.contains(criteria.query, ignoreCase = true) ||
                        item.category.contains(criteria.query, ignoreCase = true) ||
                        item.rawBody.contains(criteria.query, ignoreCase = true) ||
                        item.amount.toString().contains(criteria.query) ||
                        item.monthKey.contains(criteria.query, ignoreCase = true)
            }
            val matchesCategory = criteria.categoryFilter == null || item.category.equals(criteria.categoryFilter, ignoreCase = true)
            val matchesAmount = when (criteria.amountRange) {
                AmountRange.ALL -> true
                AmountRange.UNDER_500 -> item.amount < 500.0
                AmountRange.MID_500_2000 -> item.amount in 500.0..2000.0
                AmountRange.OVER_2000 -> item.amount > 2000.0
            }
            val matchesRecurring = !criteria.onlyRecurring || item.isRecurring
            val matchesAccount = criteria.accountFilter == null || item.accountInfo.contains(criteria.accountFilter, ignoreCase = true)

            matchesFilter && matchesQuery && matchesCategory && matchesAmount && matchesRecurring && matchesAccount
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Trailing 30-Day Median Transaction Spend for Anomaly Benchmarks
    val trailingMedianSpend: StateFlow<Double> = allExpenses.map { list ->
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val recent = list.filter {
            it.timestamp >= thirtyDaysAgo &&
            !it.isReversal &&
            it.type != ExpenseType.SELF &&
            !it.category.equals("Self", ignoreCase = true) &&
            !it.category.equals("Credit Card Bill", ignoreCase = true)
        }.map { (it.amount - it.refundedAmount).coerceAtLeast(0.0) }.sorted()

        if (recent.isEmpty()) 500.0 else {
            val mid = recent.size / 2
            if (recent.size % 2 == 0 && mid > 0) {
                (recent[mid - 1] + recent[mid]) / 2.0
            } else {
                recent[mid]
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 500.0
    )

    // Predicted Recurring Commitments & Subscriptions Flow
    val predictedRecurringBills: StateFlow<List<PredictedRecurringBill>> = combine(
        allExpenses,
        _selectedMonthKey,
        _ignoredRecurringMerchants
    ) { allList, monthKey, ignored ->
        RecurringDetectionEngine.detectRecurringBills(allList, monthKey, ignored)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Adaptive Burn Rate & Safe Daily Spend Dynamic Pacing
    val safeSpendPacing: StateFlow<SafeSpendPacing> = combine(
        currentMonthExpenses,
        _monthlyBudget,
        predictedRecurringBills,
        _blacklistedMerchants
    ) { expenses, budget, recurring, blacklisted ->
        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val daysRemaining = (daysInMonth - currentDay + 1).coerceAtLeast(1)

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var currentSpent = 0.0
        var todaySpent = 0.0

        for (exp in expenses) {
            val norm = exp.merchantOrRecipient.trim().lowercase(Locale.US)
            if (blacklisted.any { norm.contains(it.lowercase(Locale.US)) }) continue
            if (isSelf(exp) || isCreditCard(exp)) continue

            val net = (exp.amount - exp.refundedAmount).coerceAtLeast(0.0)
            currentSpent += net
            if (exp.timestamp >= startOfToday) {
                todaySpent += net
            }
        }

        val upcomingRecurring = recurring.filter { !it.isPaidThisMonth }.sumOf { it.expectedAmount }
        val remainingDiscretionary = (budget - currentSpent - upcomingRecurring).coerceAtLeast(0.0)

        val safeDaily = if (budget > 0) remainingDiscretionary / daysRemaining else 0.0

        val status = when {
            safeDaily <= 0.0 -> if (todaySpent > 0) PacingStatus.OVER_PACED else PacingStatus.ON_TRACK
            todaySpent <= safeDaily -> PacingStatus.ON_TRACK
            todaySpent <= safeDaily * 1.25 -> PacingStatus.CAUTION
            else -> PacingStatus.OVER_PACED
        }

        SafeSpendPacing(
            safeDailySpend = safeDaily,
            todaySpent = todaySpent,
            daysRemaining = daysRemaining,
            daysInMonth = daysInMonth,
            currentDay = currentDay,
            status = status,
            remainingDiscretionary = remainingDiscretionary,
            upcomingRecurringTotal = upcomingRecurring
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SafeSpendPacing()
    )

    // Blacklisted merchants are completely ignored and not considered in spend totals
    val blacklistedDeductions: StateFlow<Double> = MutableStateFlow(0.0).asStateFlow()

    // Monthly Total = all valid transactions from non-blacklisted merchants, excluding Self and Credit Card Bill
    val monthlyTotal: StateFlow<Double> = combine(
        currentMonthExpenses,
        _blacklistedMerchants
    ) { list, blacklisted ->
        val validList = list.filter {
            !isBlacklistedMerchant(it.merchantOrRecipient, blacklisted) &&
            !isSelf(it) &&
            !isCreditCard(it)
        }
        validList.sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0.0) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val debitsTotal: StateFlow<Double> = MutableStateFlow(0.0).asStateFlow()

    val transfersTotal: StateFlow<Double> = combine(
        currentMonthExpenses,
        _blacklistedMerchants
    ) { list, blacklisted ->
        list.filter { isTransfer(it) && !isBlacklistedMerchant(it.merchantOrRecipient, blacklisted) }
            .sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0.0) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val spendsTotal: StateFlow<Double> = combine(
        currentMonthExpenses,
        _blacklistedMerchants
    ) { list, blacklisted ->
        list.filter { isMerchantSpend(it) && !isBlacklistedMerchant(it.merchantOrRecipient, blacklisted) }
            .sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0.0) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val creditCardsTotal: StateFlow<Double> = combine(
        currentMonthExpenses,
        _blacklistedMerchants
    ) { list, blacklisted ->
        list.filter { isCreditCard(it) && !isBlacklistedMerchant(it.merchantOrRecipient, blacklisted) }
            .sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0.0) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val selfTotal: StateFlow<Double> = combine(
        currentMonthExpenses,
        _blacklistedMerchants
    ) { list, blacklisted ->
        list.filter { isSelf(it) && !isBlacklistedMerchant(it.merchantOrRecipient, blacklisted) }
            .sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0.0) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Live savings for selected month = Monthly Salary - Net Spend
    val monthlySavings: StateFlow<Double> = combine(
        _monthlySalary,
        monthlyTotal
    ) { salary, spent ->
        (salary - spent)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Feature 2: Deterministic Auto-Rules flow
    val merchantRules: StateFlow<List<MerchantRuleEntity>> = repository.allMerchantRules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Feature 4: Intra-Month Daily Spending Velocity & Burn-Down Curve
    val dailyBurnDownData: StateFlow<DailyBurnDownData> = combine(
        currentMonthExpenses,
        _monthlyBudget,
        _blacklistedMerchants,
        _selectedMonthKey
    ) { expenses, budget, blacklisted, monthKey ->
        val cal = Calendar.getInstance()
        val currentMonthKey = currentDefaultMonth
        val isCurrentMonth = monthKey == currentMonthKey

        val parts = monthKey.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val month0 = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1

        val monthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month0)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = if (isCurrentMonth) cal.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth) else daysInMonth

        val daySpendMap = mutableMapOf<Int, Double>()
        for (exp in expenses) {
            if (isBlacklistedMerchant(exp.merchantOrRecipient, blacklisted) || isSelf(exp) || isCreditCard(exp)) continue
            val expCal = Calendar.getInstance().apply { timeInMillis = exp.timestamp }
            val day = expCal.get(Calendar.DAY_OF_MONTH)
            val netAmt = (exp.amount - exp.refundedAmount).coerceAtLeast(0.0)
            daySpendMap[day] = (daySpendMap[day] ?: 0.0) + netAmt
        }

        val points = mutableListOf<DaySpendPoint>()
        var runningCumulative = 0.0
        val targetDailySlope = if (budget > 0) budget / daysInMonth else 0.0

        for (d in 1..currentDay) {
            val spentToday = daySpendMap[d] ?: 0.0
            runningCumulative += spentToday
            points.add(
                DaySpendPoint(
                    dayOfMonth = d,
                    daySpent = spentToday,
                    cumulativeSpend = runningCumulative,
                    targetPacingSpend = targetDailySlope * d
                )
            )
        }

        val burnRate = if (currentDay > 0) runningCumulative / currentDay else 0.0
        val projected = burnRate * daysInMonth
        val overPaced = budget > 0 && runningCumulative > (targetDailySlope * currentDay)

        DailyBurnDownData(
            points = points,
            currentDay = currentDay,
            daysInMonth = daysInMonth,
            currentCumulativeSpend = runningCumulative,
            monthlyBudget = budget,
            projectedMonthEndSpend = projected,
            isOverPaced = overPaced,
            currentBurnRatePerDay = burnRate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyBurnDownData()
    )

    // Feature 5: Multi-Account & Instrument Liquidity Intelligence
    val instrumentSummaries: StateFlow<List<InstrumentSpendSummary>> = combine(
        currentMonthExpenses,
        _blacklistedMerchants
    ) { expenses, blacklisted ->
        val valid = expenses.filter {
            !isBlacklistedMerchant(it.merchantOrRecipient, blacklisted) && !isSelf(it)
        }
        val netMonthTotal = valid.sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0.0) }
        val grouped = valid.groupBy {
            val acc = it.accountInfo.trim()
            if (acc.isNotBlank()) acc else "Other / Cash"
        }

        grouped.map { (account, list) ->
            val total = list.sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0.0) }
            val type = InstrumentType.fromAccountInfo(account)
            val pct = if (netMonthTotal > 0) (total / netMonthTotal) * 100.0 else 0.0
            InstrumentSpendSummary(
                accountInfo = account,
                instrumentType = type,
                totalSpent = total,
                transactionCount = list.size,
                percentageOfTotal = pct
            )
        }.sortedByDescending { it.totalSpent }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 12-Month Historical Analytics Flow for the Spend vs Savings Graph (deducting blacklisted merchants, self, and credit cards)
    val last12MonthsAnalytics: StateFlow<List<MonthAnalytics>> = combine(
        repository.allExpenses,
        _monthlySalary,
        _blacklistedMerchants
    ) { allExpensesList, salary, blacklisted ->
        computeLast12MonthsAnalytics(allExpensesList, salary, blacklisted)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun computeLast12MonthsAnalytics(
        expenses: List<ExpenseEntity>,
        salary: Double,
        blacklisted: Set<String>
    ): List<MonthAnalytics> {
        val calendar = Calendar.getInstance()
        val sdfKey = SimpleDateFormat("yyyy-MM", Locale.US)
        val sdfFull = SimpleDateFormat("MMM yyyy", Locale.US)
        val sdfShort = SimpleDateFormat("MMM", Locale.US)

        val months = mutableListOf<MonthAnalytics>()

        for (i in 11 downTo 0) {
            val cal = Calendar.getInstance().apply {
                time = calendar.time
                add(Calendar.MONTH, -i)
            }
            val monthKey = sdfKey.format(cal.time)
            val fullLabel = sdfFull.format(cal.time)
            val shortLabel = sdfShort.format(cal.time)
            val year = cal.get(Calendar.YEAR)
            val monthNum = cal.get(Calendar.MONTH) + 1

            val totalSpent = expenses.filter {
                it.monthKey == monthKey &&
                !isBlacklistedMerchant(it.merchantOrRecipient, blacklisted) &&
                !isSelf(it) &&
                !isCreditCard(it)
            }.sumOf { (it.amount - it.refundedAmount).coerceAtLeast(0.0) }
            val savedAmount = salary - totalSpent
            val isOverspent = totalSpent > salary
            val rate = if (salary > 0) (savedAmount / salary) * 100.0 else 0.0

            months.add(
                MonthAnalytics(
                    monthKey = monthKey,
                    monthLabel = fullLabel,
                    shortLabel = shortLabel,
                    year = year,
                    monthNumber = monthNum,
                    totalSpent = totalSpent,
                    salary = salary,
                    savedAmount = savedAmount,
                    isOverspent = isOverspent,
                    savingsRate = rate
                )
            )
        }
        val monthsWithData = months.filter { m ->
            m.totalSpent > 0.0 || expenses.any { it.monthKey == m.monthKey }
        }
        return if (monthsWithData.isNotEmpty()) monthsWithData else months.takeLast(1)
    }

    init {
        viewModelScope.launch {
            repository.importInitialSampleDataIfNeeded()
        }
    }

    fun isBlacklistedMerchant(merchant: String, blacklisted: Set<String> = _blacklistedMerchants.value): Boolean {
        val norm = merchant.trim()
        if (norm.isBlank()) return false
        return blacklisted.any { it.equals(norm, ignoreCase = true) }
    }

    fun blacklistMerchant(merchant: String) {
        val norm = merchant.trim()
        if (norm.isBlank()) return
        preferences.blacklistMerchant(norm)
        _blacklistedMerchants.value = preferences.getBlacklistedMerchants()
        _syncFeedback.value = "Merchant '$norm' blacklisted. Spends deducted."
    }

    fun unblacklistMerchant(merchant: String) {
        val norm = merchant.trim()
        preferences.unblacklistMerchant(norm)
        _blacklistedMerchants.value = preferences.getBlacklistedMerchants()
        _syncFeedback.value = "Merchant '$norm' removed from blacklist."
    }

    fun toggleBlacklistMerchant(merchant: String) {
        if (isBlacklistedMerchant(merchant)) {
            unblacklistMerchant(merchant)
        } else {
            blacklistMerchant(merchant)
        }
    }

    fun setTab(tab: SavioScreenTab) {
        _currentTab.value = tab
    }

    fun selectMonth(monthKey: String) {
        _selectedMonthKey.value = monthKey
    }

    fun updateSalary(salary: Double) {
        preferences.monthlySalary = salary
        _monthlySalary.value = salary
        _syncFeedback.value = "Salary updated: ${currency.value}${String.format(Locale.US, "%,.0f", salary)}"
    }

    fun updateSavingsGoal(goal: Double) {
        preferences.savingsGoal = goal
        _savingsGoal.value = goal
        _syncFeedback.value = "Savings goal set: ${currency.value}${String.format(Locale.US, "%,.0f", goal)}"
    }

    fun setFilter(filter: ExpenseFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun syncSmsInbox() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncFeedback.value = "Scanning & AI-validating SMS messages..."
            try {
                val count = repository.syncInbox { current, total ->
                    _syncFeedback.value = "Validating transactions with AI ($current/$total)..."
                }
                _syncFeedback.value = if (count > 0) {
                    "AI validated & synced $count new expenditure(s) from SMS"
                } else {
                    "SMS scan complete. No new expenditures detected."
                }
            } catch (e: Exception) {
                _syncFeedback.value = "Error scanning messages: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearFeedback() {
        _syncFeedback.value = null
    }

    fun parseAndAddMessage(rawBody: String, sender: String = "BankSMS"): Boolean {
        var success = false
        viewModelScope.launch {
            success = repository.parseAndAddMessage(rawBody, sender)
            if (success) {
                _syncFeedback.value = "Message parsed & added live!"
            } else {
                _syncFeedback.value = "Could not detect debit/spend in this message."
            }
        }
        return success
    }

    fun simulateSample(sample: SampleSmsData.SampleSms) {
        viewModelScope.launch {
            val success = repository.parseAndAddMessage(sample.body, sample.sender)
            if (success) {
                _syncFeedback.value = "Simulated: ${sample.description}"
            }
        }
    }

    fun togglePersistentNotification(enable: Boolean) {
        preferences.isPersistentNotificationEnabled = enable
        _isPersistentNotificationEnabled.value = enable
        val context = getApplication<Application>().applicationContext
        if (enable) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
            _syncFeedback.value = "Status bar notification enabled"
        } else {
            LiveExpenditureNotificationService.stopNotification(context)
            _syncFeedback.value = "Status bar notification stopped"
        }
    }

    fun updateBudget(budget: Double) {
        preferences.monthlyBudget = budget
        _monthlyBudget.value = budget
        val context = getApplication<Application>().applicationContext
        if (_isPersistentNotificationEnabled.value) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
    }

    fun updateCurrency(currency: String) {
        preferences.currency = currency
        _currency.value = currency
        val context = getApplication<Application>().applicationContext
        if (_isPersistentNotificationEnabled.value) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
    }

    fun updateApiKey(key: String) {
        preferences.openRouterApiKey = key
        _openRouterApiKey.value = key
        _syncFeedback.value = if (key.isNotBlank()) "OpenRouter API Key saved" else "API Key cleared"
    }

    fun updateCategoryLimits(limits: Map<String, Double>) {
        for (cat in com.example.ai.OpenRouterCategorizer.KNOWN_CATEGORIES) {
            val limit = limits[cat] ?: 0.0
            preferences.setCategoryLimit(cat, limit)
        }
        _categoryLimits.value = preferences.getAllCategoryLimits()
        _syncFeedback.value = "Category spend limits updated"
    }

    fun assignCategory(
        expenseId: Long,
        category: String,
        alias: String? = null,
        saveAsRule: Boolean = true
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>() as SpendTrackerApplication
            val dao = app.database.expenseDao()
            val ruleDao = app.database.merchantRuleDao()
            val target = dao.getExpenseById(expenseId)

            val newType = when {
                category.equals("Self", ignoreCase = true) -> ExpenseType.SELF
                category.equals("Credit Card Bill", ignoreCase = true) -> ExpenseType.CREDIT_CARD
                category.equals("Transfers", ignoreCase = true) -> ExpenseType.P2P
                else -> ExpenseType.MERCHANT
            }

            dao.updateCategoryAndType(expenseId, category, newType)

            if (target != null && target.merchantOrRecipient.isNotBlank()) {
                val merchant = target.merchantOrRecipient.trim()
                val effectiveAlias = if (!alias.isNullOrBlank()) alias.trim() else merchant
                preferences.saveMerchantCategory(merchant, category)
                if (saveAsRule) {
                    ruleDao.insertRule(
                        MerchantRuleEntity(
                            merchantPattern = merchant,
                            assignedCategory = category,
                            normalizedAlias = effectiveAlias,
                            isRegex = false,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
                dao.updateCategoryAndTypeForMerchant(merchant, category, newType)
                _syncFeedback.value = "Rule saved: '$merchant' categorized as '$category'"
            } else {
                _syncFeedback.value = "Category assigned: $category"
            }

            val updatedExpense = dao.getExpenseById(expenseId)
            if (updatedExpense != null) {
                com.example.service.ExpenseProcessingHelper.checkCategoryLimitAlert(app, updatedExpense)
            }
        }
    }

    fun addMerchantRule(pattern: String, category: String, alias: String = "", isRegex: Boolean = false) {
        viewModelScope.launch {
            val rule = MerchantRuleEntity(
                merchantPattern = pattern.trim(),
                assignedCategory = category.trim(),
                normalizedAlias = alias.trim(),
                isRegex = isRegex,
                createdAt = System.currentTimeMillis()
            )
            repository.insertMerchantRule(rule)
            _syncFeedback.value = "Auto-rule added for '$pattern' -> $category"
        }
    }

    fun deleteMerchantRule(id: Long) {
        viewModelScope.launch {
            repository.deleteMerchantRule(id)
            _syncFeedback.value = "Rule deleted"
        }
    }

    fun updateMerchantName(expenseId: Long, oldMerchant: String, newMerchant: String, category: String) {
        viewModelScope.launch {
            repository.updateMerchantName(expenseId, oldMerchant, newMerchant, category)
            _syncFeedback.value = "Merchant updated to '$newMerchant' and auto-rule saved"
        }
    }

    fun selectAccountFilter(account: String?) {
        _selectedAccountFilter.value = if (_selectedAccountFilter.value == account) null else account
    }

    fun addManualExpense(
        amount: Double,
        merchant: String,
        category: String,
        type: ExpenseType,
        accountInfo: String
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val app = context as SpendTrackerApplication
            val dao = app.database.expenseDao()

            val effectiveCategory = if (category.isNotBlank() && !category.equals("Uncategorized", ignoreCase = true)) {
                category
            } else {
                preferences.getMerchantCategory(merchant) ?: "General Spend"
            }

            if (merchant.isNotBlank() && effectiveCategory.isNotBlank()) {
                preferences.saveMerchantCategory(merchant, effectiveCategory)
            }

            val entity = ExpenseEntity(
                amount = amount,
                currency = preferences.currency,
                type = type,
                merchantOrRecipient = merchant,
                accountInfo = accountInfo,
                category = effectiveCategory,
                rawBody = "Manual Entry: $merchant $effectiveCategory",
                sender = "Manual",
                timestamp = System.currentTimeMillis()
            )
            val id = dao.insertExpense(entity)
            _syncFeedback.value = "Added $merchant (${preferences.currency}$amount)"

            val inserted = entity.copy(id = id)
            com.example.service.ExpenseProcessingHelper.checkCategoryLimitAlert(context, inserted)

            if (preferences.isPersistentNotificationEnabled) {
                LiveExpenditureNotificationService.updateLiveExpenditure(context)
            }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            _syncFeedback.value = "Expense removed"
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
            _syncFeedback.value = "All expenses cleared"
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        preferences.isBiometricLockEnabled = enabled
        _isBiometricLockEnabled.value = enabled
        _syncFeedback.value = if (enabled) "Biometric Lock enabled" else "Biometric Lock disabled"
    }

    fun setPrivacyShieldEnabled(enabled: Boolean) {
        preferences.isPrivacyShieldEnabled = enabled
        _isPrivacyShieldEnabled.value = enabled
        _syncFeedback.value = if (enabled) "Privacy Shield enabled" else "Privacy Shield disabled"
    }

    fun setLockTimeoutSeconds(seconds: Int) {
        preferences.lockTimeoutSeconds = seconds
        _lockTimeoutSeconds.value = seconds
        val desc = when (seconds) {
            0 -> "Immediately"
            30 -> "After 30s"
            60 -> "After 1m"
            300 -> "After 5m"
            else -> "$seconds seconds"
        }
        _syncFeedback.value = "Lock timeout: $desc"
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun setAmountRange(range: AmountRange) {
        _selectedAmountRange.value = range
    }

    fun setOnlyRecurringFilter(only: Boolean) {
        _onlyRecurringFilter.value = only
    }

    fun setSearchExpanded(expanded: Boolean) {
        _isSearchExpanded.value = expanded
        if (!expanded) {
            _searchQuery.value = ""
        }
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedFilter.value = ExpenseFilter.ALL
        _selectedCategoryFilter.value = null
        _selectedAmountRange.value = AmountRange.ALL
        _onlyRecurringFilter.value = false
        _selectedAccountFilter.value = null
        _isGlobalSearch.value = false
    }

    fun toggleGlobalSearch(enabled: Boolean) {
        _isGlobalSearch.value = enabled
    }

    fun toggleVelocityAlerts(enabled: Boolean) {
        preferences.isVelocityAlertsEnabled = enabled
        _isVelocityAlertsEnabled.value = enabled
        _syncFeedback.value = if (enabled) "Proactive spend velocity alerts enabled" else "Velocity alerts disabled"
    }

    fun toggleAnomalyAlerts(enabled: Boolean) {
        preferences.isAnomalyAlertsEnabled = enabled
        _isAnomalyAlertsEnabled.value = enabled
        _syncFeedback.value = if (enabled) "High-value spend anomaly alerts enabled" else "Anomaly alerts disabled"
    }

    fun toggleRecurring(expenseId: Long, isRecurring: Boolean) {
        viewModelScope.launch {
            repository.updateIsRecurring(expenseId, isRecurring)
            if (isRecurring) {
                val expense = allExpenses.value.find { it.id == expenseId }
                expense?.let {
                    val m = it.merchantOrRecipient.trim()
                    if (_ignoredRecurringMerchants.value.any { ign -> ign.equals(m, ignoreCase = true) }) {
                        preferences.removeIgnoredRecurringMerchant(m)
                        _ignoredRecurringMerchants.value = preferences.getIgnoredRecurringMerchants()
                    }
                }
            }
            _syncFeedback.value = if (isRecurring) "Marked as recurring commitment" else "Removed recurring mark"
            LiveExpenditureNotificationService.updateLiveExpenditure(getApplication())
        }
    }

    fun toggleRecurringForMerchant(merchant: String, isRecurring: Boolean) {
        viewModelScope.launch {
            repository.updateIsRecurringForMerchant(merchant, isRecurring)
            if (isRecurring) {
                if (_ignoredRecurringMerchants.value.any { ign -> ign.equals(merchant.trim(), ignoreCase = true) }) {
                    preferences.removeIgnoredRecurringMerchant(merchant.trim())
                    _ignoredRecurringMerchants.value = preferences.getIgnoredRecurringMerchants()
                }
            }
            _syncFeedback.value = if (isRecurring) "Marked all '$merchant' recurring" else "Unmarked all '$merchant' recurring"
            LiveExpenditureNotificationService.updateLiveExpenditure(getApplication())
        }
    }

    fun removeRecurringBill(merchant: String) {
        viewModelScope.launch {
            preferences.addIgnoredRecurringMerchant(merchant)
            _ignoredRecurringMerchants.value = preferences.getIgnoredRecurringMerchants()
            repository.updateIsRecurringForMerchant(merchant, false)
            _syncFeedback.value = "Removed '$merchant' from recurring radar"
            LiveExpenditureNotificationService.updateLiveExpenditure(getApplication())
        }
    }

    fun restoreRecurringBill(merchant: String) {
        viewModelScope.launch {
            preferences.removeIgnoredRecurringMerchant(merchant)
            _ignoredRecurringMerchants.value = preferences.getIgnoredRecurringMerchants()
            _syncFeedback.value = "Restored '$merchant' to recurring radar"
            LiveExpenditureNotificationService.updateLiveExpenditure(getApplication())
        }
    }

    fun deleteMonthData(monthKey: String) {
        viewModelScope.launch {
            val count = repository.deleteExpensesForMonth(monthKey)
            _syncFeedback.value = "Deleted $count transactions for ${ExpenseEntity.formatMonthDisplay(monthKey)}"
            if (_selectedMonthKey.value == monthKey) {
                _selectedMonthKey.value = currentDefaultMonth
            }
            LiveExpenditureNotificationService.updateLiveExpenditure(getApplication())
        }
    }

    fun getExpensesForMerchant(merchant: String): Flow<List<ExpenseEntity>> {
        return repository.getExpensesForMerchant(merchant)
    }

    class Factory(
        private val application: Application,
        private val repository: ExpenseRepository,
        private val preferences: ExpensePreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                return ExpenseViewModel(application, repository, preferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
