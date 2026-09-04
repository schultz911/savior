package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.OpenRouterCategorizer
import com.example.data.ExpenseEntity
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldBorder
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignCategoryDialog(
    expense: ExpenseEntity,
    currency: String,
    onAssign: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(expense.category.ifBlank { "Groceries" }) }
    var customCategory by remember { mutableStateOf("") }
    var isCustom by remember { mutableStateOf(false) }

    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Assign Spend Category",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SavioSlateDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${expense.merchantOrRecipient} • $currency${numberFormatter.format(expense.amount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SavioEmerald
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SavioEmeraldContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SavioEmeraldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: '${expense.merchantOrRecipient}' will be remembered and tagged with this category for all transactions going forward.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = SavioEmerald,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select an accurate category:",
                    style = MaterialTheme.typography.bodySmall,
                    color = SavioSlateMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OpenRouterCategorizer.KNOWN_CATEGORIES.forEach { cat ->
                        val isChosen = !isCustom && selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isChosen) SavioEmerald else SavioEmeraldContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clickable {
                                    isCustom = false
                                    selectedCategory = cat
                                }
                                .testTag("choose_cat_$cat")
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = if (isChosen) androidx.compose.ui.graphics.Color.White else SavioEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = customCategory,
                    onValueChange = {
                        customCategory = it
                        if (it.isNotBlank()) isCustom = true
                    },
                    label = { Text("Or enter custom category") },
                    placeholder = { Text("e.g. Pet Care, Gadgets") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SavioEmerald,
                        unfocusedBorderColor = GlassCardBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalCategory = if (isCustom && customCategory.isNotBlank()) {
                        customCategory.trim()
                    } else {
                        selectedCategory
                    }
                    onAssign(expense.id, finalCategory)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald)
            ) {
                Text("Save & Tag Merchant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SavioSlateDark)
            }
        }
    )
}
