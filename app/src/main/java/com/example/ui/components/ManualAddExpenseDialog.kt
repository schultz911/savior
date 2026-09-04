package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.OpenRouterCategorizer
import com.example.data.ExpenseType
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import com.example.ui.theme.SavioSpendRose

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualAddExpenseDialog(
    currency: String,
    onAddExpense: (amount: Double, merchant: String, category: String, type: ExpenseType, accountInfo: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var merchantInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }
    var selectedType by remember { mutableStateOf(ExpenseType.SPEND) }
    var accountInfoInput by remember { mutableStateOf("Cash / Manual") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Transaction",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SavioSlateDark
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Record spends not captured by SMS alerts (e.g. cash, untracked cards).",
                    style = MaterialTheme.typography.bodySmall,
                    color = SavioSlateMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amount
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it.filter { char -> char.isDigit() || char == '.' }
                        errorMessage = null
                    },
                    label = { Text("Amount ($currency)") },
                    placeholder = { Text("e.g. 350.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SavioEmerald,
                        unfocusedBorderColor = GlassCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Merchant / Payee
                OutlinedTextField(
                    value = merchantInput,
                    onValueChange = {
                        merchantInput = it
                        errorMessage = null
                    },
                    label = { Text("Merchant or Payee") },
                    placeholder = { Text("e.g. Local Market, Taxi, Swiggy") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_merchant_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SavioEmerald,
                        unfocusedBorderColor = GlassCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Type Chips: Spend, Debit, Transfer
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = SavioSlateDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseType.entries.forEach { type ->
                        val isSel = selectedType == type
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SavioEmerald,
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = SavioSlateDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OpenRouterCategorizer.KNOWN_CATEGORIES.take(7).forEach { cat ->
                        val isChosen = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isChosen) SavioEmerald else SavioEmeraldContainer.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp
                                ),
                                color = if (isChosen) androidx.compose.ui.graphics.Color.White else SavioEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSpendRose
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    if (merchantInput.isBlank()) {
                        errorMessage = "Please specify a merchant or payee name"
                        return@Button
                    }
                    onAddExpense(
                        amt,
                        merchantInput.trim(),
                        selectedCategory,
                        selectedType,
                        accountInfoInput.trim()
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald)
            ) {
                Text("Add Spend")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SavioSlateDark)
            }
        }
    )
}
