package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.OpenRouterCategorizer
import com.example.data.ExpenseType
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldBorder
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import com.example.ui.theme.SavioSpendRose

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualAddExpenseDialog(
    currency: String,
    onAddExpense: (amount: Double, merchant: String, category: String, type: ExpenseType, accountInfo: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var amountInput by remember { mutableStateOf("") }
    var merchantInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }
    var selectedType by remember { mutableStateOf(ExpenseType.SPEND) }
    var accountInfoInput by remember { mutableStateOf("Cash / Manual") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val paymentMethods = listOf("Cash / Manual", "UPI", "Credit Card", "Debit Card", "Bank Account")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
            .testTag("manual_add_bottom_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SavioEmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = SavioEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Add Transaction",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = SavioSlateDark
                        )
                        Text(
                            text = "Manual spend or offline transfer",
                            style = MaterialTheme.typography.bodySmall,
                            color = SavioSlateMuted
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SavioSlateMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Amount Input with Currency
            OutlinedTextField(
                value = amountInput,
                onValueChange = {
                    amountInput = it.filter { char -> char.isDigit() || char == '.' }
                    errorMessage = null
                },
                label = { Text("Amount ($currency)") },
                placeholder = { Text("0.00") },
                prefix = {
                    Text(
                        text = "$currency ",
                        fontWeight = FontWeight.Bold,
                        color = SavioEmerald,
                        fontSize = 18.sp
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_amount_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SavioEmerald,
                    unfocusedBorderColor = GlassCardBorder,
                    focusedLabelColor = SavioEmerald
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Merchant / Payee
            OutlinedTextField(
                value = merchantInput,
                onValueChange = {
                    merchantInput = it
                    errorMessage = null
                },
                label = { Text("Merchant or Payee") },
                placeholder = { Text("e.g. Swiggy, Local Grocery, Petrol Pump, Ramesh") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_merchant_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SavioEmerald,
                    unfocusedBorderColor = GlassCardBorder,
                    focusedLabelColor = SavioEmerald
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Type Selector (Spend vs Transfer)
            Text(
                text = "Transaction Type",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SavioSlateDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExpenseType.entries.forEach { type ->
                    val isSel = selectedType == type
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedType = type },
                        label = {
                            Text(
                                text = type.displayName,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SavioEmerald,
                            selectedLabelColor = Color.White,
                            containerColor = GlassBackground,
                            labelColor = SavioSlateDark
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection Chips (Including UPI, Groceries, Food, etc.)
            Text(
                text = "Spend Category",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SavioSlateDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OpenRouterCategorizer.KNOWN_CATEGORIES.forEach { cat ->
                    val isChosen = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isChosen) SavioEmerald else SavioEmeraldContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isChosen) SavioEmerald else SavioEmeraldBorder
                        ),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = if (isChosen) Color.White else SavioEmerald,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment / Account Method
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SavioSlateDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentMethods.forEach { method ->
                    val isChosen = accountInfoInput == method
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isChosen) SavioSlateDark else GlassBackground,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isChosen) SavioSlateDark else GlassCardBorder
                        ),
                        modifier = Modifier.clickable { accountInfoInput = method }
                    ) {
                        Text(
                            text = method,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.5.sp
                            ),
                            color = if (isChosen) Color.White else SavioSlateDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SavioSpendRose.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = SavioSpendRose,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button: Save Transaction
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
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_manual_expense_button")
            ) {
                Text(
                    text = "Save Transaction",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
