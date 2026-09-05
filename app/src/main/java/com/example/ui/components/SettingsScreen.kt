package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    modifier: Modifier = Modifier
) {
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

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SavioEmeraldContainer,
                        modifier = Modifier.clickable { onNavigateBack() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = SavioEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = SavioEmerald
                            )
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

        // 3. Blacklisted Merchants Manager Card (Feature Request)
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
                            placeholder = { Text("Enter merchant name to blacklist") },
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

        // 4. Per-Category Spend Limits Card
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
                    Text(
                        text = "Alerts triggered at 80% usage and when overshooting the limit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OpenRouterCategorizer.KNOWN_CATEGORIES.take(6).forEach { category ->
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

        // 5. OpenRouter AI Integration Card
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

        // 6. Live Status Bar Notification Card
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

        // 7. Reset / Danger Zone Card
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
                        text = "Reset all locally tracked transactions and simulated data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showConfirmClear = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SavioSpendRose),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Transactions")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
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
