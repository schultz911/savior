package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.platform.LocalContext
import com.example.data.ExpenseEntity
import com.example.data.MerchantRuleEntity
import com.example.util.ExcelExportHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.OpenRouterCategorizer
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioBlacklistBg
import com.example.ui.theme.SavioBlacklistRed
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldBorder
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSlateBody
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import com.example.ui.theme.SavioSlateSubtle
import com.example.ui.theme.SavioSpendRose

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentCurrency: String,
    currentSalary: Double,
    currentSavingsGoal: Double,
    currentBudget: Double,
    currentApiKey: String,
    categoryLimits: Map<String, Double>,
    blacklistedMerchants: Set<String>,
    isNotificationActive: Boolean,
    onUpdateCurrency: (String) -> Unit,
    onUpdateSalary: (Double) -> Unit,
    onUpdateSavingsGoal: (Double) -> Unit,
    onUpdateBudget: (Double) -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onUpdateCategoryLimits: (Map<String, Double>) -> Unit,
    onAddBlacklistedMerchant: (String) -> Unit,
    onRemoveBlacklistedMerchant: (String) -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    onClearAll: () -> Unit,
    onNavigateBack: () -> Unit,
    allExpenses: List<ExpenseEntity> = emptyList(),
    isBiometricLockEnabled: Boolean = false,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    isPrivacyShieldEnabled: Boolean = false,
    onTogglePrivacyShield: (Boolean) -> Unit = {},
    lockTimeoutSeconds: Int = 0,
    onUpdateLockTimeout: (Int) -> Unit = {},
    onTriggerBackupExport: (passphrase: String) -> Unit = {},
    onTriggerBackupRestore: (passphrase: String) -> Unit = {},
    onCreateSnapshot: (() -> Unit)? = null,
    onRestoreSnapshot: (() -> Unit)? = null,
    merchantRules: List<MerchantRuleEntity> = emptyList(),
    onAddMerchantRule: (pattern: String, category: String, alias: String) -> Unit = { _, _, _ -> },
    onDeleteMerchantRule: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isIgnoringBattery by remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        mutableStateOf(pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                isIgnoringBattery = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var isCategoryLimitsExpanded by remember { mutableStateOf(false) }
    var isRulesExpanded by remember { mutableStateOf(false) }

    var newRulePatternInput by remember { mutableStateOf("") }
    var newRuleCategoryInput by remember { mutableStateOf("Food & Dining") }
    var newRuleAliasInput by remember { mutableStateOf("") }

    var showBackupExportDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var backupPassphraseInput by remember { mutableStateOf("") }
    var restorePassphraseInput by remember { mutableStateOf("") }
    var showBackupPassphraseText by remember { mutableStateOf(false) }
    var showRestorePassphraseText by remember { mutableStateOf(false) }

    var salaryInput by remember(currentSalary) {
        mutableStateOf(if (currentSalary > 0) currentSalary.toInt().toString() else "")
    }
    var savingsGoalInput by remember(currentSavingsGoal) {
        mutableStateOf(if (currentSavingsGoal > 0) currentSavingsGoal.toInt().toString() else "")
    }
    var budgetInput by remember(currentBudget) {
        mutableStateOf(if (currentBudget > 0) currentBudget.toInt().toString() else "")
    }
    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var showApiKeyText by remember { mutableStateOf(false) }

    var selectedCurrency by remember(currentCurrency) { mutableStateOf(currentCurrency) }
    var showConfirmClear by remember { mutableStateOf(false) }

    var newBlacklistInput by remember { mutableStateOf("") }

    val localCategoryLimits = remember(categoryLimits) {
        val map = mutableStateMapOf<String, String>()
        for (cat in OpenRouterCategorizer.KNOWN_CATEGORIES) {
            val limit = categoryLimits[cat] ?: 0.0
            if (limit > 0) {
                map[cat] = limit.toInt().toString()
            }
        }
        map
    }

    val currencyOptions = listOf("₹", "$", "€", "£", "CA$", "AU$")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GlassBackground)
            .testTag("settings_full_window"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Settings Header Banner with Glassmorphic styling
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SavioEmeraldContainer,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SavioEmeraldBorder),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = SavioEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Settings & Preferences",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                ),
                                color = SavioSlateDark
                            )
                            Text(
                                text = "Fine-tune Savio₹ to match your lifestyle",
                                style = MaterialTheme.typography.bodySmall,
                                color = SavioSlateMuted
                            )
                        }
                    }
                }
            }
        }

        // 1. Live Status Bar Notification Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = SavioEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live Status Bar Spend Tracker",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SavioSlateDark
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Keeps live total spend & monthly budget status in your notifications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SavioSlateMuted
                        )
                    }

                    Switch(
                        checked = isNotificationActive,
                        onCheckedChange = { onToggleNotification(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SavioEmerald
                        )
                    )
                }
            }
        }

        // 2. Background SMS Reliability & OEM Doze Protection Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
                    .testTag("battery_optimization_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isIgnoringBattery) SavioEmeraldContainer else Color(0xFFFEF3C7),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isIgnoringBattery) Icons.Default.BatteryChargingFull else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isIgnoringBattery) SavioEmerald else Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SMS Reliability & Doze Protection",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SavioSlateDark
                                )
                                Text(
                                    text = if (isIgnoringBattery) "Unrestricted Background Execution" else "Optimized (May delay SMS detection)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isIgnoringBattery) SavioEmerald else Color(0xFFD97706),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isIgnoringBattery) SavioEmerald.copy(alpha = 0.12f) else Color(0xFFFEF3C7),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isIgnoringBattery) SavioEmerald.copy(alpha = 0.3f) else Color(0xFFF59E0B).copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = if (isIgnoringBattery) "ACTIVE" else "RESTRICTED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = if (isIgnoringBattery) SavioEmerald else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Aggressive OEM battery optimizations (MIUI, OxygenOS, ColorOS, OneUI) can freeze background SMS receivers and periodic sync workers when your device is asleep. Disabling battery optimizations ensures 100% instant transaction capture.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )

                    if (!isIgnoringBattery) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        context.startActivity(fallback)
                                    } catch (_: Exception) {}
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald)
                        ) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Allow Unrestricted Background Execution")
                        }
                    }
                }
            }
        }

        // 2. Financial Baseline & Savings Target Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = SavioEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Income & Savings Targets",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SavioSlateDark
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Currency Selector
                    Text(
                        text = "Preferred Currency",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SavioSlateDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencyOptions.forEach { curr ->
                            val isSel = selectedCurrency == curr
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedCurrency = curr
                                    onUpdateCurrency(curr)
                                },
                                label = {
                                    Text(
                                        text = curr,
                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SavioEmerald,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Monthly Take-home Salary
                    Text(
                        text = "Monthly Take-Home Salary ($selectedCurrency)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SavioSlateDark
                    )
                    Text(
                        text = "Savings are calculated live as: Salary minus Net Monthly Spends",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = salaryInput,
                            onValueChange = { input ->
                                salaryInput = input.filter { it.isDigit() || it == '.' }
                            },
                            placeholder = { Text("e.g. 75000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SavioEmerald,
                                unfocusedBorderColor = GlassCardBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val d = salaryInput.toDoubleOrNull() ?: 0.0
                                onUpdateSalary(d)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald)
                        ) {
                            Text("Save")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Monthly Savings Goal
                    Text(
                        text = "Monthly Savings Goal ($selectedCurrency)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SavioSlateDark
                    )
                    Text(
                        text = "Target amount you intend to save every month",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = savingsGoalInput,
                            onValueChange = { input ->
                                savingsGoalInput = input.filter { it.isDigit() || it == '.' }
                            },
                            placeholder = { Text("e.g. 25000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SavioEmerald,
                                unfocusedBorderColor = GlassCardBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val d = savingsGoalInput.toDoubleOrNull() ?: 0.0
                                onUpdateSavingsGoal(d)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald)
                        ) {
                            Text("Save")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Monthly Spend Budget Cap
                    Text(
                        text = "Monthly Spend Cap ($selectedCurrency)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SavioSlateDark
                    )
                    Text(
                        text = "Used for the live notification status progress bar",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = budgetInput,
                            onValueChange = { input ->
                                budgetInput = input.filter { it.isDigit() || it == '.' }
                            },
                            placeholder = { Text("e.g. 40000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SavioEmerald,
                                unfocusedBorderColor = GlassCardBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val d = budgetInput.toDoubleOrNull() ?: 0.0
                                onUpdateBudget(d)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        // 3. Per-Category Spend Limits Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCategoryLimitsExpanded = !isCategoryLimitsExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = SavioEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Per-Category Spend Limits",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SavioSlateDark
                            )
                        }

                        IconButton(
                            onClick = { isCategoryLimitsExpanded = !isCategoryLimitsExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isCategoryLimitsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isCategoryLimitsExpanded) "Collapse" else "Expand",
                                tint = SavioEmerald
                            )
                        }
                    }

                    Text(
                        text = "Alerts triggered at 80% usage and when overshooting the limit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )

                    AnimatedVisibility(visible = isCategoryLimitsExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            OpenRouterCategorizer.KNOWN_CATEGORIES.filterNot {
                                it.equals("Self", ignoreCase = true) || it.equals("Credit Card Bill", ignoreCase = true)
                            }.forEach { category ->
                                val currentVal = localCategoryLimits[category] ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = SavioSlateDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = currentVal,
                                        onValueChange = { newVal ->
                                            val filtered = newVal.filter { it.isDigit() }
                                            localCategoryLimits[category] = filtered
                                        },
                                        placeholder = { Text("0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        prefix = { Text(selectedCurrency) },
                                        modifier = Modifier.width(130.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SavioEmerald,
                                            unfocusedBorderColor = GlassCardBorder
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val map = mutableMapOf<String, Double>()
                                    for ((cat, str) in localCategoryLimits) {
                                        val v = str.toDoubleOrNull() ?: 0.0
                                        if (v > 0) map[cat] = v
                                    }
                                    onUpdateCategoryLimits(map)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald)
                            ) {
                                Text("Update Category Limits")
                            }
                        }
                    }
                }
            }
        }

        // 4. Merchant Rules & Aliases (Deterministic Local Classifier) Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
                    .testTag("merchant_rules_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SavioEmeraldContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = SavioEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Merchant Rules & Aliases",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SavioSlateDark
                            )
                            Text(
                                text = "Deterministic local classifier for 100% offline auto-categorization",
                                style = MaterialTheme.typography.bodySmall,
                                color = SavioSlateMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Input Form for new rule
                    Text(
                        text = "Add Merchant Rule",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pattern",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = SavioSlateDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = newRulePatternInput,
                                onValueChange = { newRulePatternInput = it },
                                placeholder = { Text("e.g. SWIGGY*") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SavioEmerald,
                                    unfocusedBorderColor = GlassCardBorder
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alias",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = SavioSlateDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = newRuleAliasInput,
                                onValueChange = { newRuleAliasInput = it },
                                placeholder = { Text("e.g. Swiggy") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SavioEmerald,
                                    unfocusedBorderColor = GlassCardBorder
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick category selection chips for new rule
                    Text(
                        text = "Assigned Category: $newRuleCategoryInput",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = SavioEmerald
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OpenRouterCategorizer.KNOWN_CATEGORIES.take(6).forEach { cat ->
                            val isSel = newRuleCategoryInput.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = { newRuleCategoryInput = cat },
                                label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SavioEmerald,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (newRulePatternInput.isNotBlank()) {
                                onAddMerchantRule(
                                    newRulePatternInput.trim(),
                                    newRuleCategoryInput,
                                    newRuleAliasInput.trim()
                                )
                                newRulePatternInput = ""
                                newRuleAliasInput = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Classification Rule")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // List of Active Rules (Collapsible)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRulesExpanded = !isRulesExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Active Rules (${merchantRules.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SavioSlateDark
                        )
                        Icon(
                            imageVector = if (isRulesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isRulesExpanded) "Collapse rules" else "Expand rules",
                            tint = SavioSlateDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isRulesExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (merchantRules.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = GlassBackground,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No auto-rules active. Add a rule above or check 'Save as auto-rule' when categorizing a spend.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SavioSlateMuted,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    merchantRules.forEach { rule ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White.copy(alpha = 0.6f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = rule.merchantPattern,
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = SavioSlateDark
                                                        )
                                                        if (rule.normalizedAlias.isNotBlank() && rule.normalizedAlias != rule.merchantPattern) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "➔ \"${rule.normalizedAlias}\"",
                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                                color = SavioEmerald
                                                            )
                                                        }
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = SavioEmeraldContainer,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = rule.assignedCategory,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                            color = SavioEmerald,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { onDeleteMerchantRule(rule.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Delete rule",
                                                        tint = SavioSlateMuted,
                                                        modifier = Modifier.size(16.dp)
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
            }
        }

        // 5. Blacklisted Merchants Manager Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
                    .testTag("blacklisted_merchants_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = SavioBlacklistRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Blacklisted Merchants",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SavioSlateDark
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transactions from blacklisted merchants are completely ignored and not considered in spend totals or savings calculations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Input to add a new merchant to blacklist
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newBlacklistInput,
                            onValueChange = { newBlacklistInput = it },
                            label = { Text("Enter merchant name") },
                            placeholder = { Text("Enter merchant name") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SavioBlacklistRed,
                                unfocusedBorderColor = GlassCardBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newBlacklistInput.isNotBlank()) {
                                    onAddBlacklistedMerchant(newBlacklistInput.trim())
                                    newBlacklistInput = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SavioBlacklistRed)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Blacklist")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // List of current blacklisted merchants
                    if (blacklistedMerchants.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GlassBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No merchants blacklisted. Blacklisted merchants will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SavioSlateMuted,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            blacklistedMerchants.forEach { merchant ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SavioBlacklistBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SavioBlacklistRed.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = merchant,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SavioBlacklistRed
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove $merchant",
                                            tint = SavioBlacklistRed,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onRemoveBlacklistedMerchant(merchant) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Security & Privacy Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = SavioEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security & Privacy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SavioSlateDark
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Biometric Lock Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometric App Lock",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SavioSlateDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Require fingerprint or screen lock to open Savio₹.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SavioSlateMuted
                            )
                        }

                        Switch(
                            checked = isBiometricLockEnabled,
                            onCheckedChange = { onToggleBiometricLock(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SavioEmerald
                            )
                        )
                    }

                    if (isBiometricLockEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Lock Timeout",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SavioSlateMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0 to "Immediately",
                                30 to "30s",
                                60 to "1m",
                                300 to "5m"
                            ).forEach { (seconds, label) ->
                                FilterChip(
                                    selected = lockTimeoutSeconds == seconds,
                                    onClick = { onUpdateLockTimeout(seconds) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SavioEmeraldContainer,
                                        selectedLabelColor = SavioEmerald
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = GlassCardBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Privacy Shield Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recents Privacy Shield",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SavioSlateDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hide app preview in Android recent apps switcher.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SavioSlateMuted
                            )
                        }

                        Switch(
                            checked = isPrivacyShieldEnabled,
                            onCheckedChange = { onTogglePrivacyShield(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SavioEmerald
                            )
                        )
                    }
                }
            }
        }

        // 7. OpenRouter AI Integration Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SavioEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OpenRouter AI Integration",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SavioSlateDark
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (apiKeyInput.isNotBlank()) SavioEmeraldContainer else GlassBackground
                        ) {
                            Text(
                                text = if (apiKeyInput.isNotBlank()) "ACTIVE" else "LOCAL",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (apiKeyInput.isNotBlank()) SavioEmerald else SavioSlateMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Uses gemini-3.5-flash-lite to parse bank SMS (spends, transfers, OTPs, ads) and categorize transactions automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it.trim() },
                        label = { Text("OpenRouter API Key") },
                        placeholder = { Text("sk-or-v1-...") },
                        visualTransformation = if (showApiKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showApiKeyText = !showApiKeyText }) {
                                Text(if (showApiKeyText) "Hide" else "Show", fontSize = 12.sp, color = SavioEmerald)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("openrouter_api_key_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SavioEmerald,
                            unfocusedBorderColor = GlassCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onUpdateApiKey(apiKeyInput) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald)
                    ) {
                        Text("Save API Key")
                    }
                }
            }
        }

        // 8. Data Management Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Export your transaction history to Excel or reset local data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                ExcelExportHelper.exportExpensesToExcel(context, allExpenses)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export Excel",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export (.XLS)")
                        }

                        Button(
                            onClick = { showConfirmClear = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SavioSpendRose),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Clear All",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear All")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = GlassCardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Encrypted Vault Backup",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create an AES-256-GCM encrypted .savior container to back up your expenses, settings, and merchant metadata securely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                backupPassphraseInput = ""
                                showBackupExportDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SavioSlateDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Backup Vault",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup Vault", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                restorePassphraseInput = ""
                                showBackupRestoreDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SavioSlateDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "Restore Vault",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Vault", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Auto-Rotated Local Snapshots (Last 3 Saved)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark
                    )
                    Text(
                        text = "Instant 1-tap backup stored safely inside device sandbox without needing a file picker.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { onCreateSnapshot?.invoke() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Take Snapshot",
                                modifier = Modifier.size(16.dp),
                                tint = SavioEmerald
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Snapshot", fontSize = 12.sp, color = SavioSlateDark)
                        }

                        androidx.compose.material3.OutlinedButton(
                            onClick = { onRestoreSnapshot?.invoke() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "Restore Snapshot",
                                modifier = Modifier.size(16.dp),
                                tint = SavioSlateDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Latest", fontSize = 12.sp, color = SavioSlateDark)
                        }
                    }
                }
            }
        }

        // 8. Savior Branding Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_savio_logo),
                    contentDescription = "Savior Logo",
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = SavioSlateDark,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = (-0.3).sp
                            )
                        ) {
                            append("Savio")
                        }
                        withStyle(
                            SpanStyle(
                                color = SavioEmerald,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = (-0.3).sp
                            )
                        ) {
                            append("₹")
                        }
                    }
                )

                Text(
                    text = "\"Be the savior your money needs.\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontWeight = FontWeight.Medium
                    ),
                    color = SavioEmerald
                )

                Text(
                    text = "Developed by Kiran Saldanha",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = SavioSlateMuted
                )

                Text(
                    text = "v1.2.0 (vNext)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp
                    ),
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }

    if (showBackupExportDialog) {
        AlertDialog(
            onDismissRequest = { showBackupExportDialog = false },
            title = {
                Text(
                    text = "Export Encrypted Backup",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SavioSlateDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a strong passphrase to encrypt your backup container (AES-256-GCM). You will need this passphrase to restore your data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SavioSlateBody
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = backupPassphraseInput,
                        onValueChange = { backupPassphraseInput = it },
                        label = { Text("Backup Passphrase") },
                        singleLine = true,
                        visualTransformation = if (showBackupPassphraseText) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showBackupPassphraseText = !showBackupPassphraseText }) {
                                Icon(
                                    imageVector = if (showBackupPassphraseText) Icons.Default.Close else Icons.Default.Lock,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SavioEmerald,
                            focusedLabelColor = SavioEmerald
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pass = backupPassphraseInput.trim()
                        if (pass.isNotEmpty()) {
                            showBackupExportDialog = false
                            onTriggerBackupExport(pass)
                        }
                    },
                    enabled = backupPassphraseInput.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Export Container")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupExportDialog = false }) {
                    Text("Cancel", color = SavioSlateDark)
                }
            }
        )
    }

    if (showBackupRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showBackupRestoreDialog = false },
            title = {
                Text(
                    text = "Restore Encrypted Backup",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SavioSlateDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the passphrase used when creating the .savior backup file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SavioSlateBody
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = restorePassphraseInput,
                        onValueChange = { restorePassphraseInput = it },
                        label = { Text("Decryption Passphrase") },
                        singleLine = true,
                        visualTransformation = if (showRestorePassphraseText) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showRestorePassphraseText = !showRestorePassphraseText }) {
                                Icon(
                                    imageVector = if (showRestorePassphraseText) Icons.Default.Close else Icons.Default.Lock,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SavioEmerald,
                            focusedLabelColor = SavioEmerald
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pass = restorePassphraseInput.trim()
                        if (pass.isNotEmpty()) {
                            showBackupRestoreDialog = false
                            onTriggerBackupRestore(pass)
                        }
                    },
                    enabled = restorePassphraseInput.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = SavioSlateDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Select & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupRestoreDialog = false }) {
                    Text("Cancel", color = SavioSlateDark)
                }
            }
        )
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = {
                Text(
                    text = "Clear All Expenditures?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SavioSlateDark
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all tracked transactions from local storage. Are you sure?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SavioSlateBody
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showConfirmClear = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SavioSpendRose),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Yes, Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Cancel", color = SavioSlateDark)
                }
            }
        )
    }
}
