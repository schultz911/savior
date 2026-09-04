package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.OpenRouterCategorizer
import com.example.ui.theme.BentoDebitRed
import com.example.ui.theme.BentoLavenderContainer
import com.example.ui.theme.BentoPurpleDark
import com.example.ui.theme.BentoPurplePrimary

@Composable
fun SettingsDialog(
    currentCurrency: String,
    currentSalary: Double,
    currentSavingsGoal: Double,
    currentBudget: Double,
    currentApiKey: String,
    categoryLimits: Map<String, Double>,
    isNotificationActive: Boolean,
    onUpdateCurrency: (String) -> Unit,
    onUpdateSalary: (Double) -> Unit,
    onUpdateSavingsGoal: (Double) -> Unit,
    onUpdateBudget: (Double) -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onUpdateCategoryLimits: (Map<String, Double>) -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var salaryInput by remember {
        mutableStateOf(if (currentSalary > 0) currentSalary.toInt().toString() else "")
    }
    var savingsGoalInput by remember {
        mutableStateOf(if (currentSavingsGoal > 0) currentSavingsGoal.toInt().toString() else "")
    }
    var budgetInput by remember {
        mutableStateOf(if (currentBudget > 0) currentBudget.toInt().toString() else "")
    }
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var showApiKeyText by remember { mutableStateOf(false) }

    var selectedCurrency by remember { mutableStateOf(currentCurrency) }
    var notificationEnabled by remember { mutableStateOf(isNotificationActive) }
    var showConfirmClear by remember { mutableStateOf(false) }

    // Map for editing category limits
    val localCategoryLimits = remember {
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settings & AI Configuration",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // OpenRouter & Gemini AI Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = BentoPurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OpenRouter AI Integration",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = BentoPurpleDark
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Uses gemini-3.5-flash-lite to categorize bank & card SMS spends automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it.trim() },
                    label = { Text("OpenRouter API Key") },
                    placeholder = { Text("sk-or-v1-...") },
                    visualTransformation = if (showApiKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showApiKeyText = !showApiKeyText }) {
                            Text(if (showApiKeyText) "Hide" else "Show", fontSize = 11.sp)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("openrouter_api_key_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Monthly Salary
                Text(
                    text = "Monthly Take-Home Salary",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Used to calculate your net savings = Salary - Total Spends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = salaryInput,
                    onValueChange = { salaryInput = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Monthly Salary ($selectedCurrency)") },
                    placeholder = { Text("e.g. 75000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Monthly Savings Goal
                Text(
                    text = "Monthly Savings Goal",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Target amount you want to save every month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = savingsGoalInput,
                    onValueChange = { savingsGoalInput = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Savings Goal ($selectedCurrency)") },
                    placeholder = { Text("e.g. 25000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Per-Category Spend Limits
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = BentoPurplePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Per-Category Spend Limits",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Alerts triggered at 80% usage and when overshooting the limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Category limit inputs for top categories
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
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = currentVal,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }
                                if (clean.isEmpty()) {
                                    localCategoryLimits.remove(category)
                                } else {
                                    localCategoryLimits[category] = clean
                                }
                            },
                            placeholder = { Text("No limit", fontSize = 11.sp) },
                            prefix = { Text(selectedCurrency, fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .width(120.dp)
                                .height(52.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Currency Selector
                Text(
                    text = "Currency Symbol",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currencyOptions.forEach { curr ->
                        FilterChip(
                            selected = selectedCurrency == curr,
                            onClick = { selectedCurrency = curr },
                            label = { Text(curr) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Monthly Budget Goal (Overall Limit)
                Text(
                    text = "Overall Monthly Spend Budget",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Overall Limit ($selectedCurrency)") },
                    placeholder = { Text("e.g. 50000 (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status Bar Notification Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Persistent Status Bar Tracker",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Always displays live monthly spend total in status bar with quick '+ Add Spend' action",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = notificationEnabled,
                        onCheckedChange = { notificationEnabled = it },
                        modifier = Modifier.testTag("settings_notification_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reset data
                TextButton(
                    onClick = { showConfirmClear = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = BentoDebitRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Tracked Expenses")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateCurrency(selectedCurrency)
                    val sal = salaryInput.toDoubleOrNull() ?: 0.0
                    onUpdateSalary(sal)
                    val sGoal = savingsGoalInput.toDoubleOrNull() ?: 0.0
                    onUpdateSavingsGoal(sGoal)
                    val budget = budgetInput.toDoubleOrNull() ?: 0.0
                    onUpdateBudget(budget)
                    onUpdateApiKey(apiKeyInput.trim())

                    // Save parsed category limits
                    val convertedLimits = mutableMapOf<String, Double>()
                    for ((k, v) in localCategoryLimits) {
                        val num = v.toDoubleOrNull() ?: 0.0
                        if (num > 0) convertedLimits[k] = num
                    }
                    onUpdateCategoryLimits(convertedLimits)

                    onToggleNotification(notificationEnabled)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Clear All Expenses?") },
            text = { Text("This will permanently delete all parsed expenditures from the local database.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showConfirmClear = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoDebitRed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
