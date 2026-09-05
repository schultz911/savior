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

enum class ExpenseFilter(val label: String) {
    ALL("All"),
    SPENDS("Spends"),
    TRANSFERS("Transfers"),
    CREDIT_CARDS("Credit Cards"),
    SELF("Self")
}

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

    val filteredExpenses: StateFlow<List<ExpenseEntity>> = combine(
        currentMonthExpenses,
        _selectedFilter,
        _searchQuery
    ) { expenses, filter, query ->
        expenses.filter { item ->
            val matchesFilter = when (filter) {
                ExpenseFilter.ALL -> true
                ExpenseFilter.SPENDS -> isMerchantSpend(item)
                ExpenseFilter.TRANSFERS -> isTransfer(item)
                ExpenseFilter.CREDIT_CARDS -> isCreditCard(item)
                ExpenseFilter.SELF -> isSelf(item)
            }
            val matchesQuery = if (query.isBlank()) true else {
                item.merchantOrRecipient.contains(query, ignoreCase = true) ||
                        item.accountInfo.contains(query, ignoreCase = true) ||
                        item.category.contains(query, ignoreCase = true) ||
                        item.rawBody.contains(query, ignoreCase = true) ||
                        item.amount.toString().contains(query)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
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
        validList.sumOf { it.amount }
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
            .sumOf { it.amount }
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
            .sumOf { it.amount }
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
            .sumOf { it.amount }
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
            .sumOf { it.amount }
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
            }.sumOf { it.amount }
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
        return months
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

    fun assignCategory(expenseId: Long, category: String) {
        viewModelScope.launch {
            val app = getApplication<Application>() as SpendTrackerApplication
            val dao = app.database.expenseDao()
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
                preferences.saveMerchantCategory(merchant, category)
                dao.updateCategoryAndTypeForMerchant(merchant, category, newType)
                _syncFeedback.value = "Category '$category' saved for '$merchant' going forward"
            } else {
                _syncFeedback.value = "Category assigned: $category"
            }

            val updatedExpense = dao.getExpenseById(expenseId)
            if (updatedExpense != null) {
                com.example.service.ExpenseProcessingHelper.checkCategoryLimitAlert(app, updatedExpense)
            }
        }
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
