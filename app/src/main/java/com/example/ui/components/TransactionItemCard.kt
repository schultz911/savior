package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseEntity
import com.example.data.ExpenseType
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioBlacklistBg
import com.example.ui.theme.SavioBlacklistMuted
import com.example.ui.theme.SavioBlacklistRed
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSlateBody
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import com.example.ui.theme.SavioSpendRose
import com.example.ui.theme.SavioSpendRoseBg
import com.example.ui.theme.SavioTransferIndigo
import com.example.ui.theme.SavioTransferIndigoBg
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionItemCard(
    expense: ExpenseEntity,
    currency: String = expense.currency,
    isBlacklisted: Boolean = false,
    onDelete: (Long) -> Unit,
    onAssignCategory: ((ExpenseEntity) -> Unit)? = null,
    onToggleBlacklist: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDetailDialog by remember { mutableStateOf(false) }

    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val effectiveCurrency = if (currency.isNotBlank()) currency else expense.currency
    val formattedAmount = "-${effectiveCurrency}${numberFormatter.format(expense.amount)}"

    val dateFormatter = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.US) }
    val formattedDate = dateFormatter.format(Date(expense.timestamp))

    val (typeColor, typeBg, _) = when (expense.type) {
        ExpenseType.TRANSFER -> Triple(SavioTransferIndigo, SavioTransferIndigoBg, Icons.Default.SwapHoriz)
        ExpenseType.SPEND -> Triple(SavioSpendRose, SavioSpendRoseBg, Icons.Default.CreditCard)
    }

    val categoryIcon = getCategoryIcon(expense.category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}")
            .clickable { showDetailDialog = true },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category / Type Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isBlacklisted) SavioBlacklistBg else typeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBlacklisted) Icons.Default.Block else categoryIcon,
                    contentDescription = expense.category,
                    tint = if (isBlacklisted) SavioBlacklistRed else typeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Transaction Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.merchantOrRecipient,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textDecoration = if (isBlacklisted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isBlacklisted) SavioBlacklistMuted else SavioSlateDark,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (isBlacklisted) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SavioBlacklistBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SavioBlacklistRed.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Ignored",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = SavioBlacklistRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        // Type Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = typeBg
                        ) {
                            Text(
                                text = expense.type.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = typeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateMuted
                    )

                    if (expense.accountInfo.isNotBlank()) {
                        Text(
                            text = " • ${expense.accountInfo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SavioSlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount & Interactive Category Tag
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textDecoration = if (isBlacklisted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isBlacklisted) SavioBlacklistMuted else typeColor
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Interactive Category Tag for EVERY spend (per user request)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (expense.category.equals("Uncategorized", ignoreCase = true)) {
                        SavioSpendRoseBg
                    } else {
                        SavioEmeraldContainer
                    },
                    modifier = Modifier.clickable {
                        onAssignCategory?.invoke(expense)
                    }
                ) {
                    Text(
                        text = "${expense.category} ✎",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = if (expense.category.equals("Uncategorized", ignoreCase = true)) {
                            SavioSpendRose
                        } else {
                            SavioEmerald
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }

    if (showDetailDialog) {
        TransactionDetailDialog(
            expense = expense,
            formattedAmount = formattedAmount,
            formattedDate = formattedDate,
            isBlacklisted = isBlacklisted,
            onDismiss = { showDetailDialog = false },
            onChangeCategory = {
                showDetailDialog = false
                onAssignCategory?.invoke(expense)
            },
            onToggleBlacklist = {
                onToggleBlacklist?.invoke(expense.merchantOrRecipient)
                showDetailDialog = false
            },
            onDelete = {
                onDelete(expense.id)
                showDetailDialog = false
            }
        )
    }
}

@Composable
private fun TransactionDetailDialog(
    expense: ExpenseEntity,
    formattedAmount: String,
    formattedDate: String,
    isBlacklisted: Boolean,
    onDismiss: () -> Unit,
    onChangeCategory: () -> Unit,
    onToggleBlacklist: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SavioSlateDark
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Transaction",
                        tint = SavioSpendRose
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow("Merchant / Title", expense.merchantOrRecipient)
                DetailRow("Amount", formattedAmount)
                DetailRow("Type", expense.type.displayName)
                DetailRow("Category", expense.category)
                if (isBlacklisted) {
                    DetailRow("Status", "Blacklisted (Excluded from Spends)")
                }
                if (expense.accountInfo.isNotBlank()) {
                    DetailRow("Account / Card", expense.accountInfo)
                }
                if (expense.sender.isNotBlank()) {
                    DetailRow("SMS Sender", expense.sender)
                }
                DetailRow("Date", formattedDate)

                if (expense.rawBody.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Original SMS Body:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GlassCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = expense.rawBody,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            color = SavioSlateBody
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions row inside details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onChangeCategory,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Category", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onToggleBlacklist,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBlacklisted) SavioEmerald else SavioBlacklistRed
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBlacklisted) "Unblacklist" else "Blacklist", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SavioSlateDark)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SavioSlateMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = SavioSlateDark
        )
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Groceries" -> Icons.Default.ShoppingCart
        "Shopping" -> Icons.Default.ShoppingBag
        "Food & Dining" -> Icons.Default.Fastfood
        "Bills & Utilities" -> Icons.Default.Receipt
        "Travel & Commute" -> Icons.Default.Flight
        "Transfers" -> Icons.Default.SwapHoriz
        else -> Icons.Default.CreditCard
    }
}
