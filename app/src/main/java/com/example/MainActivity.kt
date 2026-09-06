package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.DailyBurnDownChart
import com.example.ui.components.ExpenditureHeroCard
import com.example.ui.components.InstrumentLiquidityCard
import com.example.ui.components.ManualAddExpenseDialog
import com.example.ui.components.MonthSelector
import com.example.ui.components.PermissionsBanner
import com.example.ui.components.RecurringCommitmentsSheet
import com.example.ui.components.SettingsScreen
import com.example.ui.components.SpendBreakupPieChartCard
import com.example.ui.components.TestSmsBottomSheet
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldBorder
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSlateBody
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import com.example.ui.theme.SavioSpendRose
import com.example.ui.theme.SavioTransferIndigo

import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import com.example.security.AppSecurityManager
import com.example.util.DatabaseBackupHelper
import com.example.ui.components.BiometricLockOverlay
import com.example.ui.components.MerchantDetailSheet
import com.example.ui.components.SearchFilterBar
import com.example.ai.OpenRouterCategorizer

class MainActivity : FragmentActivity() {

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
                    manualAddTrigger = manualAddTrigger,
                    initialAssignExpenseId = assignCategoryExpenseIdState
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val app = application as SpendTrackerApplication
        if (app.preferences.isPrivacyShieldEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        AppSecurityManager.onAppForegrounded(
            app.preferences.isBiometricLockEnabled,
            app.preferences.lockTimeoutSeconds
        )
    }

    override fun onStop() {
        super.onStop()
        AppSecurityManager.onAppBackgrounded()
    }

    private var manualAddTrigger by mutableStateOf(0L)
    private var assignCategoryExpenseIdState by mutableStateOf<Long?>(null)

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent != null) {
            if (intent.action == ACTION_ADD_SPEND ||
                intent.action == LiveExpenditureNotificationService.ACTION_ADD_SPEND ||
                intent.getBooleanExtra(EXTRA_SHOW_MANUAL_ADD, false)) {
                manualAddTrigger = System.currentTimeMillis()
            }
            if (intent.hasExtra(EXTRA_ASSIGN_CATEGORY_EXPENSE_ID)) {
                assignCategoryExpenseIdState = intent.getLongExtra(EXTRA_ASSIGN_CATEGORY_EXPENSE_ID, -1L)
            }
        }
    }

    companion object {
        const val ACTION_ADD_SPEND = "com.example.savior.ACTION_ADD_SPEND"
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
    manualAddTrigger: Long = 0L,
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
    val transfersTotal by viewModel.transfersTotal.collectAsStateWithLifecycle()
    val spendsTotal by viewModel.spendsTotal.collectAsStateWithLifecycle()
    val creditCardsTotal by viewModel.creditCardsTotal.collectAsStateWithLifecycle()
    val selfTotal by viewModel.selfTotal.collectAsStateWithLifecycle()
    val blacklistedMerchants by viewModel.blacklistedMerchants.collectAsStateWithLifecycle()
    val monthlySavings by viewModel.monthlySavings.collectAsStateWithLifecycle()
    val monthlySalary by viewModel.monthlySalary.collectAsStateWithLifecycle()
    val savingsGoal by viewModel.savingsGoal.collectAsStateWithLifecycle()
    val last12MonthsAnalytics by viewModel.last12MonthsAnalytics.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()

    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val isNotificationActive by viewModel.isPersistentNotificationEnabled.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncFeedback by viewModel.syncFeedback.collectAsStateWithLifecycle()

    val openRouterApiKey by viewModel.openRouterApiKey.collectAsStateWithLifecycle()
    val categoryLimits by viewModel.categoryLimits.collectAsStateWithLifecycle()

    val isBiometricLockEnabled by viewModel.isBiometricLockEnabled.collectAsStateWithLifecycle()
    val isPrivacyShieldEnabled by viewModel.isPrivacyShieldEnabled.collectAsStateWithLifecycle()
    val lockTimeoutSeconds by viewModel.lockTimeoutSeconds.collectAsStateWithLifecycle()
    val isAppLocked by AppSecurityManager.isLocked.collectAsStateWithLifecycle()
    var biometricErrorMessage by remember { mutableStateOf<String?>(null) }

    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedAmountRange by viewModel.selectedAmountRange.collectAsStateWithLifecycle()
    val onlyRecurringFilter by viewModel.onlyRecurringFilter.collectAsStateWithLifecycle()
    val isSearchExpanded by viewModel.isSearchExpanded.collectAsStateWithLifecycle()
    val predictedRecurringBills by viewModel.predictedRecurringBills.collectAsStateWithLifecycle()
    val safeSpendPacing by viewModel.safeSpendPacing.collectAsStateWithLifecycle()

    val merchantRules by viewModel.merchantRules.collectAsStateWithLifecycle()
    val dailyBurnDownData by viewModel.dailyBurnDownData.collectAsStateWithLifecycle()
    val instrumentSummaries by viewModel.instrumentSummaries.collectAsStateWithLifecycle()
    val selectedAccountFilter by viewModel.selectedAccountFilter.collectAsStateWithLifecycle()

    var selectedMerchantForSheet by remember { mutableStateOf<String?>(null) }
    var showRecurringCommitmentsSheet by remember { mutableStateOf(false) }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var pendingBackupPassphrase by remember { mutableStateOf("") }
    var pendingRestorePassphrase by remember { mutableStateOf("") }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        AppSecurityManager.onActivityResultCompleted()
        if (uri != null && pendingBackupPassphrase.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        val app = context.applicationContext as SpendTrackerApplication
                        val res = DatabaseBackupHelper.createEncryptedBackup(
                            dao = app.database.expenseDao(),
                            preferences = app.preferences,
                            passphrase = pendingBackupPassphrase,
                            outputStream = outputStream,
                            ruleDao = app.database.merchantRuleDao()
                        )
                        if (res.isSuccess) {
                            snackbarHostState.showSnackbar("Vault backup saved! (${res.getOrNull()} transactions encrypted)")
                        } else {
                            snackbarHostState.showSnackbar("Backup failed: ${res.exceptionOrNull()?.message}")
                        }
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error exporting backup: ${e.localizedMessage}")
                } finally {
                    pendingBackupPassphrase = ""
                }
            }
        } else {
            pendingBackupPassphrase = ""
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        AppSecurityManager.onActivityResultCompleted()
        if (uri != null && pendingRestorePassphrase.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val app = context.applicationContext as SpendTrackerApplication
                        val res = DatabaseBackupHelper.restoreEncryptedBackup(
                            inputStream = inputStream,
                            passphrase = pendingRestorePassphrase,
                            dao = app.database.expenseDao(),
                            preferences = app.preferences,
                            ruleDao = app.database.merchantRuleDao()
                        )
                        if (res.isSuccess) {
                            snackbarHostState.showSnackbar("Successfully restored ${res.getOrNull()} expenditures!")
                        } else {
                            snackbarHostState.showSnackbar("Restore failed: ${res.exceptionOrNull()?.message}")
                        }
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error reading file: ${e.localizedMessage}")
                } finally {
                    pendingRestorePassphrase = ""
                }
            }
        } else {
            pendingRestorePassphrase = ""
        }
    }

    val activity = context as? androidx.fragment.app.FragmentActivity

    LaunchedEffect(isAppLocked, isBiometricLockEnabled) {
        if (isAppLocked && isBiometricLockEnabled && !AppSecurityManager.isAwaitingActivityResult && activity != null) {
            AppSecurityManager.promptBiometric(
                activity = activity,
                onSuccess = { biometricErrorMessage = null },
                onError = { biometricErrorMessage = it }
            )
        }
    }

    LaunchedEffect(isPrivacyShieldEnabled) {
        if (activity != null) {
            if (isPrivacyShieldEnabled) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    var showTestSmsSheet by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var assignCategoryTargetExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    LaunchedEffect(manualAddTrigger) {
        if (manualAddTrigger > 0L) {
            showManualAddDialog = true
            viewModel.setTab(SavioScreenTab.DASHBOARD)
        }
    }

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

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        AppSecurityManager.onActivityResultCompleted()
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
        AppSecurityManager.markAwaitingActivityResult()
        try {
            permissionsLauncher.launch(perms.toTypedArray())
        } catch (e: Exception) {
            AppSecurityManager.onActivityResultCompleted()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Could not request permissions: ${e.localizedMessage}")
            }
        }
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

    // Gesture back navigation: Swipe-back navigates to the main Dashboard instead of closing the app
    BackHandler(enabled = currentTab != SavioScreenTab.DASHBOARD) {
        viewModel.setTab(SavioScreenTab.DASHBOARD)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(GlassBackground),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(46.dp)
                            .clickable {
                                if (currentTab == SavioScreenTab.SETTINGS) {
                                    viewModel.setTab(SavioScreenTab.DASHBOARD)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_savio_logo),
                            contentDescription = "Savio Logo",
                            modifier = Modifier.size(42.dp)
                        )
                    }
                },
                title = {
                    // Savior Wordmark: One cohesive word centered with rupee symbol forming the 'r'
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = SavioSlateDark,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 23.sp,
                                    letterSpacing = (-0.4).sp
                                )
                            ) {
                                append("Savio")
                            }
                            withStyle(
                                SpanStyle(
                                    color = SavioEmerald,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 23.sp,
                                    letterSpacing = (-0.4).sp
                                )
                            ) {
                                append("₹")
                            }
                        },
                        modifier = Modifier.clickable {
                            if (currentTab == SavioScreenTab.SETTINGS) {
                                viewModel.setTab(SavioScreenTab.DASHBOARD)
                            }
                        }
                    )
                },
                actions = {
                    // Test SMS simulator button
                    IconButton(
                        onClick = { showTestSmsSheet = true },
                        modifier = Modifier.testTag("open_test_sms_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Test SMS Simulator",
                            tint = SavioEmerald
                        )
                    }

                    // Settings Button ONLY in header (toggles back when on settings)
                    IconButton(
                        onClick = {
                            if (currentTab == SavioScreenTab.SETTINGS) {
                                viewModel.setTab(SavioScreenTab.DASHBOARD)
                            } else {
                                viewModel.setTab(SavioScreenTab.SETTINGS)
                            }
                        },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = if (currentTab == SavioScreenTab.SETTINGS) {
                                Icons.AutoMirrored.Filled.ArrowBack
                            } else {
                                Icons.Default.Settings
                            },
                            contentDescription = "Settings",
                            tint = if (currentTab == SavioScreenTab.SETTINGS) SavioEmerald else SavioSlateDark
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GlassBackground
                )
            )
        },
        bottomBar = {
            // Custom Glassmorphic Bottom Bar with creative overshooting central Add button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Navigation Bar Surface: Solid white, seamless, no borders or mid lines
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(64.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // Left Tab: Dashboard
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setTab(SavioScreenTab.DASHBOARD) }
                                    .padding(vertical = 8.dp)
                                    .testTag("tab_dashboard"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = "Dashboard",
                                    tint = if (currentTab == SavioScreenTab.DASHBOARD) SavioEmerald else SavioSlateMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Dashboard",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (currentTab == SavioScreenTab.DASHBOARD) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (currentTab == SavioScreenTab.DASHBOARD) SavioEmerald else SavioSlateMuted
                                )
                            }

                            // Central gap for overshooting button
                            Spacer(modifier = Modifier.width(80.dp))

                            // Right Tab: Analytics (renamed from Calendar & Graph)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setTab(SavioScreenTab.ANALYTICS) }
                                    .padding(vertical = 8.dp)
                                    .testTag("tab_analytics"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Insights,
                                    contentDescription = "Analytics",
                                    tint = if (currentTab == SavioScreenTab.ANALYTICS) SavioEmerald else SavioSlateMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Analytics",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (currentTab == SavioScreenTab.ANALYTICS) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (currentTab == SavioScreenTab.ANALYTICS) SavioEmerald else SavioSlateMuted
                                )
                            }
                        }
                    }
                }

                // Creative Overshooting Add Transaction Button (Rupee Notes Wad + Plus Badge)
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .offset(y = (-18).dp)
                        .size(68.dp)
                        .clickable { showManualAddDialog = true }
                        .testTag("open_manual_add_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    CreativeRupeeWadAddButton()
                }
            }
        }
    ) { innerPadding ->
        // Main view content switched by tab - cleanly below top app bar without overlap!
        when (currentTab) {
            SavioScreenTab.SETTINGS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    SettingsScreen(
                        currentCurrency = currency,
                        currentSalary = monthlySalary,
                        currentSavingsGoal = savingsGoal,
                        currentBudget = monthlyBudget,
                        currentApiKey = openRouterApiKey,
                        categoryLimits = categoryLimits,
                        blacklistedMerchants = blacklistedMerchants,
                        isNotificationActive = isNotificationActive,
                        onUpdateCurrency = { viewModel.updateCurrency(it) },
                        onUpdateSalary = { viewModel.updateSalary(it) },
                        onUpdateSavingsGoal = { viewModel.updateSavingsGoal(it) },
                        onUpdateBudget = { viewModel.updateBudget(it) },
                        onUpdateApiKey = { viewModel.updateApiKey(it) },
                        onUpdateCategoryLimits = { viewModel.updateCategoryLimits(it) },
                        onAddBlacklistedMerchant = { viewModel.blacklistMerchant(it) },
                        onRemoveBlacklistedMerchant = { viewModel.unblacklistMerchant(it) },
                        onToggleNotification = { viewModel.togglePersistentNotification(it) },
                        onClearAll = { viewModel.clearAll() },
                        onNavigateBack = { viewModel.setTab(SavioScreenTab.DASHBOARD) },
                        allExpenses = allExpenses,
                        isBiometricLockEnabled = isBiometricLockEnabled,
                        onToggleBiometricLock = { viewModel.setBiometricLockEnabled(it) },
                        isPrivacyShieldEnabled = isPrivacyShieldEnabled,
                        onTogglePrivacyShield = { viewModel.setPrivacyShieldEnabled(it) },
                        lockTimeoutSeconds = lockTimeoutSeconds,
                        onUpdateLockTimeout = { viewModel.setLockTimeoutSeconds(it) },
                        merchantRules = merchantRules,
                        onAddMerchantRule = { pattern, cat, alias -> viewModel.addMerchantRule(pattern, cat, alias) },
                        onDeleteMerchantRule = { id -> viewModel.deleteMerchantRule(id) },
                        onTriggerBackupExport = { passphrase ->
                            pendingBackupPassphrase = passphrase
                            AppSecurityManager.markAwaitingActivityResult()
                            try {
                                exportBackupLauncher.launch(DatabaseBackupHelper.generateDefaultFileName())
                            } catch (e: Exception) {
                                AppSecurityManager.onActivityResultCompleted()
                                pendingBackupPassphrase = ""
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Could not open file picker: ${e.localizedMessage}")
                                }
                            }
                        },
                        onTriggerBackupRestore = { passphrase ->
                            pendingRestorePassphrase = passphrase
                            AppSecurityManager.markAwaitingActivityResult()
                            try {
                                restoreBackupLauncher.launch(arrayOf("*/*"))
                            } catch (e: Exception) {
                                AppSecurityManager.onActivityResultCompleted()
                                pendingRestorePassphrase = ""
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Could not open file picker: ${e.localizedMessage}")
                                }
                            }
                        },
                        onCreateSnapshot = {
                            coroutineScope.launch {
                                val app = context.applicationContext as SpendTrackerApplication
                                val res = DatabaseBackupHelper.createLocalRollingSnapshot(
                                    context = context,
                                    dao = app.database.expenseDao(),
                                    preferences = app.preferences,
                                    ruleDao = app.database.merchantRuleDao()
                                )
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Local vault snapshot created! (${res.getOrNull()?.name})")
                                } else {
                                    snackbarHostState.showSnackbar("Snapshot failed: ${res.exceptionOrNull()?.message}")
                                }
                            }
                        },
                        onRestoreSnapshot = {
                            coroutineScope.launch {
                                val app = context.applicationContext as SpendTrackerApplication
                                val res = DatabaseBackupHelper.restoreLatestLocalSnapshot(
                                    context = context,
                                    dao = app.database.expenseDao(),
                                    preferences = app.preferences,
                                    ruleDao = app.database.merchantRuleDao()
                                )
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Successfully restored ${res.getOrNull()} expenditures from latest snapshot!")
                                } else {
                                    snackbarHostState.showSnackbar("Restore snapshot failed: ${res.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    )
                }
            }

            SavioScreenTab.ANALYTICS -> {
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
                        allExpenses = allExpenses,
                        burnDownData = dailyBurnDownData,
                        instruments = instrumentSummaries,
                        selectedAccount = selectedAccountFilter,
                        onSelectAccount = { viewModel.selectAccountFilter(it) },
                        onSelectMonth = { viewModel.selectMonth(it) },
                        onNavigateToDashboard = { viewModel.setTab(SavioScreenTab.DASHBOARD) },
                        onDeleteMonth = { viewModel.deleteMonthData(it) },
                        onEditMerchant = { id, oldM, newM, cat ->
                            viewModel.updateMerchantName(id, oldM, newM, cat)
                        }
                    )
                }
            }

            SavioScreenTab.DASHBOARD -> {
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
                            transfersTotal = transfersTotal,
                            spendsTotal = spendsTotal,
                            creditCardsTotal = creditCardsTotal,
                            selfTotal = selfTotal,
                            currency = currency,
                            monthlySalary = monthlySalary,
                            monthlySavings = monthlySavings,
                            savingsGoal = savingsGoal,
                            monthlyBudget = monthlyBudget,
                            isNotificationActive = isNotificationActive,
                            onToggleNotification = { viewModel.togglePersistentNotification(it) },
                            safeSpendPacing = safeSpendPacing,
                            upcomingCommitmentsCount = predictedRecurringBills.count { !it.isPaidThisMonth },
                            onUpcomingCommitmentsClick = { showRecurringCommitmentsSheet = true }
                        )
                    }

                    // AI Category Spend Breakup Pie Chart & Limits Card
                    item {
                        SpendBreakupPieChartCard(
                            expenses = currentMonthExpenses,
                            currency = currency,
                            categoryLimits = categoryLimits,
                            blacklistedMerchants = blacklistedMerchants,
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

                    // In-Memory Search & Multi-Parametric Filter Bar
                    item {
                        SearchFilterBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            isSearchExpanded = isSearchExpanded,
                            onToggleSearchExpanded = { viewModel.setSearchExpanded(it) },
                            selectedCategory = selectedCategoryFilter,
                            onSelectCategory = { viewModel.setCategoryFilter(it) },
                            availableCategories = OpenRouterCategorizer.KNOWN_CATEGORIES,
                            selectedAmountRange = selectedAmountRange,
                            onSelectAmountRange = { viewModel.setAmountRange(it) },
                            onlyRecurring = onlyRecurringFilter,
                            onToggleOnlyRecurring = { viewModel.setOnlyRecurringFilter(it) },
                            onClearAllFilters = { viewModel.clearAllFilters() },
                            currency = currency
                        )
                    }

                    // Transaction Type Filters (All, Spends, Transfers, Credit Cards, Self)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExpenseFilter.entries.forEach { filter ->
                                val isSelected = selectedFilter == filter
                                val filterColor = when (filter) {
                                    ExpenseFilter.ALL -> SavioEmerald
                                    ExpenseFilter.SPENDS -> SavioSpendRose
                                    ExpenseFilter.TRANSFERS -> SavioTransferIndigo
                                    ExpenseFilter.CREDIT_CARDS -> Color(0xFF7C3AED)
                                    ExpenseFilter.SELF -> SavioEmerald
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
                                color = SavioSlateDark
                            )

                            Text(
                                text = "Auto-synced from SMS",
                                style = MaterialTheme.typography.bodySmall,
                                color = SavioSlateMuted
                            )
                        }
                    }

                    // Empty State
                    if (filteredExpenses.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                                border = BorderStroke(1.dp, GlassCardBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        tint = SavioSlateMuted,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "No matching transactions found"
                                        else "No expenditures recorded for this month yet",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = SavioSlateDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Incoming bank messages will automatically appear here live.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SavioSlateMuted
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
                                isBlacklisted = viewModel.isBlacklistedMerchant(expense.merchantOrRecipient, blacklistedMerchants),
                                onDelete = { viewModel.deleteExpense(it) },
                                onAssignCategory = { target ->
                                    assignCategoryTargetExpense = target
                                },
                                onToggleBlacklist = { merchant ->
                                    viewModel.toggleBlacklistMerchant(merchant)
                                },
                                onToggleRecurring = { id, isRec ->
                                    viewModel.toggleRecurring(id, isRec)
                                },
                                onOpenMerchantSheet = { merchant ->
                                    selectedMerchantForSheet = merchant
                                },
                                onEditMerchant = { id, oldM, newM, cat ->
                                    viewModel.updateMerchantName(id, oldM, newM, cat)
                                }
                            )
                        }
                    }

                    // Footer Spacer so items aren't covered by bottom bar
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
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

    // Manual Add Expense Dialog (triggered from bottom overshoot FAB or notification action)
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

    // Assign Category Dialog
    assignCategoryTargetExpense?.let { targetExpense ->
        AssignCategoryDialog(
            expense = targetExpense,
            currency = currency,
            onAssign = { id, newCategory, alias, saveAsRule ->
                viewModel.assignCategory(id, newCategory, alias, saveAsRule)
                assignCategoryTargetExpense = null
            },
            onDismiss = { assignCategoryTargetExpense = null }
        )
    }

    // Native Merchant Intelligence & Drilldown Sheet
    selectedMerchantForSheet?.let { merchant ->
        val merchantExpenses = remember(merchant, allExpenses) {
            allExpenses.filter { it.merchantOrRecipient.equals(merchant, ignoreCase = true) }
        }
        MerchantDetailSheet(
            merchantName = merchant,
            currency = currency,
            isBlacklisted = viewModel.isBlacklistedMerchant(merchant, blacklistedMerchants),
            expenses = merchantExpenses,
            onDismiss = { selectedMerchantForSheet = null },
            onToggleBlacklist = { viewModel.toggleBlacklistMerchant(it) },
            onToggleRecurring = { m, isRec -> viewModel.toggleRecurringForMerchant(m, isRec) }
        )
    }

    // Interactive Subscription Radar & Recurring Commitments Sheet
    if (showRecurringCommitmentsSheet) {
        RecurringCommitmentsSheet(
            bills = predictedRecurringBills,
            currency = currency,
            onDismiss = { showRecurringCommitmentsSheet = false },
            onToggleRecurring = { m, isRec -> viewModel.toggleRecurringForMerchant(m, isRec) },
            onRemoveRecurringBill = { viewModel.removeRecurringBill(it) }
        )
    }

    // Biometric App Lock Overlay
    BiometricLockOverlay(
        isLocked = isAppLocked && isBiometricLockEnabled && !AppSecurityManager.isAwaitingActivityResult,
        errorMessage = biometricErrorMessage,
        onAuthenticateClick = {
            if (activity != null) {
                AppSecurityManager.promptBiometric(
                    activity = activity,
                    onSuccess = { biometricErrorMessage = null },
                    onError = { biometricErrorMessage = it }
                )
            }
        }
    )
}

/**
 * Creative Wad of Rupee Notes with a Superimposed Plus Badge
 * Overshoots the bottom bar with rich layers, gradients and depth.
 */
@Composable
fun CreativeRupeeWadAddButton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Stack of Rupee Banknotes (upper portion)
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(width = 54.dp, height = 34.dp),
            contentAlignment = Alignment.Center
        ) {
            // Back note rotated -12 degrees
            Surface(
                modifier = Modifier
                    .size(width = 46.dp, height = 28.dp)
                    .rotate(-12f),
                shape = RoundedCornerShape(5.dp),
                color = Color(0xFF047857),
                border = BorderStroke(0.8.dp, Color(0xFF34D399).copy(alpha = 0.5f))
            ) {}

            // Mid note rotated +10 degrees
            Surface(
                modifier = Modifier
                    .size(width = 48.dp, height = 28.dp)
                    .rotate(10f),
                shape = RoundedCornerShape(5.dp),
                color = Color(0xFF059669),
                border = BorderStroke(0.8.dp, Color(0xFFA7F3D0).copy(alpha = 0.6f))
            ) {}

            // Front Note (straight, prominent ₹ banknote)
            Surface(
                modifier = Modifier
                    .size(width = 52.dp, height = 30.dp),
                shape = RoundedCornerShape(6.dp),
                color = SavioEmerald,
                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF10B981), Color(0xFF047857))
                            )
                        )
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }

        // Circular Plus Button in the middle, overlapping the bottom edge of the note stack
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-6).dp)
                .size(34.dp)
                .shadow(8.dp, CircleShape, ambientColor = SavioEmerald, spotColor = SavioEmerald)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.5.dp, SavioEmerald, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Spend",
                tint = SavioEmerald,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
