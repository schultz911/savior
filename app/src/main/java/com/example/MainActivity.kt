package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ExpenseEntity
import com.example.service.LiveExpenditureNotificationService
import com.example.ui.ExpenseFilter
import com.example.ui.ExpenseViewModel
import com.example.ui.SavioScreenTab
import com.example.ui.components.AssignCategoryDialog
import com.example.ui.components.CalendarAnalyticsTab
import com.example.ui.components.ExpenditureHeroCard
import com.example.ui.components.ManualAddExpenseDialog
import com.example.ui.components.MonthSelector
import com.example.ui.components.PermissionsBanner
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SpendBreakupPieChartCard
import com.example.ui.components.TestSmsBottomSheet
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.BentoCardBg
import com.example.ui.theme.BentoDebitRed
import com.example.ui.theme.BentoLavenderContainer
import com.example.ui.theme.BentoPurpleDark
import com.example.ui.theme.BentoPurplePrimary
import com.example.ui.theme.BentoSpendPlum
import com.example.ui.theme.BentoTransferPurple
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SavioLogoAccent
import com.example.ui.theme.StatusActiveGreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SpendTrackerApplication
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                val viewModel: ExpenseViewModel = viewModel(
                    factory = ExpenseViewModel.Factory(
                        application = app,
                        repository = app.repository,
                        preferences = app.preferences
                    )
                )

                SpendTrackerScreen(
                    viewModel = viewModel,
                    initialShowManualAdd = initialShowManualAdd,
                    initialAssignExpenseId = initialAssignExpenseId
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private var initialShowManualAdd: Boolean = false
    private var initialAssignExpenseId: Long? = null

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent != null) {
            initialShowManualAdd = intent.getBooleanExtra(EXTRA_SHOW_MANUAL_ADD, false)
            if (intent.hasExtra(EXTRA_ASSIGN_CATEGORY_EXPENSE_ID)) {
                initialAssignExpenseId = intent.getLongExtra(EXTRA_ASSIGN_CATEGORY_EXPENSE_ID, -1L)
            }
        }
    }

    companion object {
        const val EXTRA_SHOW_MANUAL_ADD = "extra_show_manual_add"
        const val EXTRA_ASSIGN_CATEGORY_EXPENSE_ID = "extra_assign_category_expense_id"
        const val EXTRA_NAVIGATE_TAB = "extra_navigate_tab"
        const val TAB_DASHBOARD = "tab_dashboard"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendTrackerScreen(
    viewModel: ExpenseViewModel,
    initialShowManualAdd: Boolean = false,
    initialAssignExpenseId: Long? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // State collected safely with lifecycle
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsStateWithLifecycle()
    val availableMonthKeys by viewModel.allMonthKeys.collectAsStateWithLifecycle()
    val currentMonthExpenses by viewModel.currentMonthExpenses.collectAsStateWithLifecycle()
    val filteredExpenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val monthlyTotal by viewModel.monthlyTotal.collectAsStateWithLifecycle()
    val debitsTotal by viewModel.debitsTotal.collectAsStateWithLifecycle()
    val transfersTotal by viewModel.transfersTotal.collectAsStateWithLifecycle()
    val spendsTotal by viewModel.spendsTotal.collectAsStateWithLifecycle()
    val monthlySavings by viewModel.monthlySavings.collectAsStateWithLifecycle()
    val monthlySalary by viewModel.monthlySalary.collectAsStateWithLifecycle()
    val savingsGoal by viewModel.savingsGoal.collectAsStateWithLifecycle()
    val last12MonthsAnalytics by viewModel.last12MonthsAnalytics.collectAsStateWithLifecycle()

    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val isNotificationActive by viewModel.isPersistentNotificationEnabled.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncFeedback by viewModel.syncFeedback.collectAsStateWithLifecycle()

    val openRouterApiKey by viewModel.openRouterApiKey.collectAsStateWithLifecycle()
    val categoryLimits by viewModel.categoryLimits.collectAsStateWithLifecycle()

    var showTestSmsSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(initialShowManualAdd) }
    var assignCategoryTargetExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    // Check if initialAssignExpenseId is provided from notification
    LaunchedEffect(initialAssignExpenseId, currentMonthExpenses) {
        if (initialAssignExpenseId != null && initialAssignExpenseId > 0) {
            val found = currentMonthExpenses.find { it.id == initialAssignExpenseId }
            if (found != null) {
                assignCategoryTargetExpense = found
            }
        }
    }

    // Check permissions
    fun checkHasSmsPermissions(): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val receiveGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && receiveGranted
    }

    fun checkHasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    var hasSmsPermissions by remember { mutableStateOf(checkHasSmsPermissions()) }
    var hasNotificationPermission by remember { mutableStateOf(checkHasNotificationPermission()) }

    // Permission launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasSmsPermissions = checkHasSmsPermissions()
        hasNotificationPermission = checkHasNotificationPermission()

        if (hasSmsPermissions) {
            viewModel.syncSmsInbox()
        }
        if (hasNotificationPermission && isNotificationActive) {
            LiveExpenditureNotificationService.updateLiveExpenditure(context)
        }
    }

    fun requestRequiredPermissions() {
        val perms = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(perms.toTypedArray())
    }

    // React to feedback events
    LaunchedEffect(syncFeedback) {
        syncFeedback?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BentoPurpleDark,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "S",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = SavioLogoAccent
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SAV",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = BentoPurpleDark
                            )
                            Text(
                                text = "IO",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = SavioLogoAccent
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showManualAddDialog = true },
                        modifier = Modifier.testTag("open_manual_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Spend Manually",
                            tint = BentoPurplePrimary
                        )
                    }

                    IconButton(
                        onClick = { showTestSmsSheet = true },
                        modifier = Modifier.testTag("open_test_sms_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Test SMS Simulator",
                            tint = BentoPurplePrimary
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == SavioScreenTab.DASHBOARD,
                    onClick = { viewModel.setTab(SavioScreenTab.DASHBOARD) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPurpleDark,
                        selectedTextColor = BentoPurpleDark,
                        indicatorColor = BentoLavenderContainer
                    ),
                    modifier = Modifier.testTag("tab_dashboard")
                )

                NavigationBarItem(
                    selected = currentTab == SavioScreenTab.CALENDAR_ANALYTICS,
                    onClick = { viewModel.setTab(SavioScreenTab.CALENDAR_ANALYTICS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar & Analytics"
                        )
                    },
                    label = { Text("Calendar & Graph") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPurpleDark,
                        selectedTextColor = BentoPurpleDark,
                        indicatorColor = BentoLavenderContainer
                    ),
                    modifier = Modifier.testTag("tab_calendar")
                )
            }
        }
    ) { innerPadding ->
        if (currentTab == SavioScreenTab.CALENDAR_ANALYTICS) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                CalendarAnalyticsTab(
                    currency = currency,
                    monthlySalary = monthlySalary,
                    savingsGoal = savingsGoal,
                    selectedMonthKey = selectedMonthKey,
                    last12Months = last12MonthsAnalytics,
                    currentMonthExpenses = currentMonthExpenses,
                    onSelectMonth = { viewModel.selectMonth(it) },
                    onNavigateToDashboard = { viewModel.setTab(SavioScreenTab.DASHBOARD) }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Month Selector Row
                item {
                    MonthSelector(
                        selectedMonthKey = selectedMonthKey,
                        availableMonthKeys = availableMonthKeys,
                        onSelectMonth = { viewModel.selectMonth(it) }
                    )
                }

                // Hero Live Expenditure Card
                item {
                    ExpenditureHeroCard(
                        monthDisplay = ExpenseEntity.formatMonthDisplay(selectedMonthKey),
                        totalExpenditure = monthlyTotal,
                        debitsTotal = debitsTotal,
                        transfersTotal = transfersTotal,
                        spendsTotal = spendsTotal,
                        currency = currency,
                        monthlySalary = monthlySalary,
                        monthlySavings = monthlySavings,
                        savingsGoal = savingsGoal,
                        monthlyBudget = monthlyBudget,
                        isNotificationActive = isNotificationActive,
                        onToggleNotification = { viewModel.togglePersistentNotification(it) }
                    )
                }

                // AI Category Spend Breakup Pie Chart & Limits Card
                item {
                    SpendBreakupPieChartCard(
                        expenses = currentMonthExpenses,
                        currency = currency,
                        categoryLimits = categoryLimits,
                        onCategoryClick = { categoryName ->
                            viewModel.setSearchQuery(categoryName)
                        }
                    )
                }

                // Permissions & Inbox Sync Banner
                item {
                    PermissionsBanner(
                        hasSmsPermissions = hasSmsPermissions,
                        hasNotificationPermission = hasNotificationPermission,
                        isSyncing = isSyncing,
                        onRequestPermissions = { requestRequiredPermissions() },
                        onSyncInbox = { viewModel.syncSmsInbox() }
                    )
                }

                // Filter & Search Controls
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search merchant, card, category...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Search",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_field"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filter Chips: All, Debits, Transfers, Spends
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExpenseFilter.entries.forEach { filter ->
                                val isSelected = selectedFilter == filter
                                val filterColor = when (filter) {
                                    ExpenseFilter.ALL -> BentoPurplePrimary
                                    ExpenseFilter.DEBITS -> BentoDebitRed
                                    ExpenseFilter.TRANSFERS -> BentoTransferPurple
                                    ExpenseFilter.SPENDS -> BentoSpendPlum
                                }

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setFilter(filter) },
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
                                    modifier = Modifier.testTag("filter_${filter.name}")
                                )
                            }
                        }
                    }
                }

                // Transaction List Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transactions (${filteredExpenses.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Auto-synced from SMS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Empty State if no transactions match
                if (filteredExpenses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No matching transactions found"
                                    else "No expenditures recorded for this month yet",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Incoming bank messages will automatically appear here live.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Transaction Items
                    items(
                        items = filteredExpenses,
                        key = { it.id }
                    ) { expense ->
                        TransactionItemCard(
                            expense = expense,
                            currency = currency,
                            onDelete = { viewModel.deleteExpense(it) },
                            onAssignCategory = { target ->
                                assignCategoryTargetExpense = target
                            }
                        )
                    }
                }

                // Footer Spacer
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Test SMS Simulator Bottom Sheet
    if (showTestSmsSheet) {
        TestSmsBottomSheet(
            onDismiss = { showTestSmsSheet = false },
            onParseAndAdd = { body, sender ->
                viewModel.parseAndAddMessage(body, sender)
            },
            onSimulateSample = { sample ->
                viewModel.simulateSample(sample)
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentCurrency = currency,
            currentSalary = monthlySalary,
            currentSavingsGoal = savingsGoal,
            currentBudget = monthlyBudget,
            currentApiKey = openRouterApiKey,
            categoryLimits = categoryLimits,
            isNotificationActive = isNotificationActive,
            onUpdateCurrency = { viewModel.updateCurrency(it) },
            onUpdateSalary = { viewModel.updateSalary(it) },
            onUpdateSavingsGoal = { viewModel.updateSavingsGoal(it) },
            onUpdateBudget = { viewModel.updateBudget(it) },
            onUpdateApiKey = { viewModel.updateApiKey(it) },
            onUpdateCategoryLimits = { viewModel.updateCategoryLimits(it) },
            onToggleNotification = { viewModel.togglePersistentNotification(it) },
            onClearAll = { viewModel.clearAll() },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Manual Add Expense Dialog (triggered from top bar or notification action)
    if (showManualAddDialog) {
        ManualAddExpenseDialog(
            currency = currency,
            onAddExpense = { amount, merchant, category, type, accountInfo ->
                viewModel.addManualExpense(
                    amount = amount,
                    merchant = merchant,
                    category = category,
                    type = type,
                    accountInfo = accountInfo
                )
                showManualAddDialog = false
            },
            onDismiss = { showManualAddDialog = false }
        )
    }

    // Assign Category Dialog (for unrecognized spend from push notification or tap)
    assignCategoryTargetExpense?.let { targetExpense ->
        AssignCategoryDialog(
            expense = targetExpense,
            currency = currency,
            onAssign = { id, newCategory ->
                viewModel.assignCategory(id, newCategory)
                assignCategoryTargetExpense = null
            },
            onDismiss = { assignCategoryTargetExpense = null }
        )
    }
}
