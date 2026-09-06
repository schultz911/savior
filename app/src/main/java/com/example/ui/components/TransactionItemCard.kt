package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseEntity
import com.example.data.ExpenseType
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioBlacklistBg
import com.example.ui.theme.SavioBlacklistMuted
import com.example.ui.theme.SavioBlacklistRed
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldBorder
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
import java.util.Calendar
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
    onToggleRecurring: ((Long, Boolean) -> Unit)? = null,
    onOpenMerchantSheet: ((String) -> Unit)? = null,
    onEditMerchant: ((id: Long, oldMerchant: String, newMerchant: String, category: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDetailSheet by remember { mutableStateOf(false) }

    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val effectiveCurrency = if (currency.isNotBlank()) currency else expense.currency
    val formattedAmount = "-${effectiveCurrency}${numberFormatter.format(expense.amount)}"

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val expenseYear = remember(expense.timestamp) {
        val c = Calendar.getInstance().apply { timeInMillis = expense.timestamp }
        c.get(Calendar.YEAR)
    }
    val dateFormatter = remember(expenseYear, currentYear) {
        if (expenseYear != currentYear) {
            SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.US)
        } else {
            SimpleDateFormat("MMM dd, hh:mm a", Locale.US)
        }
    }
    val formattedDate = dateFormatter.format(Date(expense.timestamp))

    val (typeColor, typeBg, _) = when (expense.type) {
        ExpenseType.P2P -> Triple(SavioTransferIndigo, SavioTransferIndigoBg, Icons.Default.SwapHoriz)
        ExpenseType.SELF -> Triple(SavioEmerald, SavioEmeraldContainer, Icons.Default.AccountBalance)
        ExpenseType.CREDIT_CARD -> Triple(Color(0xFF7C3AED), Color(0xFFF5F3FF), Icons.Default.CreditCard)
        ExpenseType.MERCHANT -> Triple(SavioSpendRose, SavioSpendRoseBg, Icons.Default.CreditCard)
    }

    val categoryIcon = getCategoryIcon(expense.category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}")
            .clickable { showDetailSheet = true },
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
                    } else if (expense.isFullyRefunded) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SavioEmeraldContainer,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SavioEmeraldBorder)
                        ) {
                            Text(
                                text = "Reversed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = SavioEmerald,
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
                if (expense.isFullyRefunded) {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textDecoration = TextDecoration.LineThrough
                        ),
                        color = SavioSlateMuted
                    )
                    Text(
                        text = "+${effectiveCurrency}${numberFormatter.format(expense.refundedAmount)} Refunded",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = SavioEmerald
                    )
                } else if (expense.isPartiallyRefunded) {
                    Text(
                        text = "-${effectiveCurrency}${numberFormatter.format(expense.netAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textDecoration = if (isBlacklisted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isBlacklisted) SavioBlacklistMuted else typeColor
                    )
                    Text(
                        text = "(${effectiveCurrency}${numberFormatter.format(expense.refundedAmount)} refunded)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = SavioEmerald
                    )
                } else {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textDecoration = if (isBlacklisted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isBlacklisted) SavioBlacklistMuted else typeColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Interactive Category & Recurring Tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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

                    if (expense.isRecurring) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.clickable {
                                onToggleRecurring?.invoke(expense.id, !expense.isRecurring)
                            }
                        ) {
                            Text(
                                text = "🔁 Bill",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFF2563EB),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDetailSheet) {
        TransactionDetailBottomSheet(
            expense = expense,
            formattedAmount = formattedAmount,
            formattedDate = formattedDate,
            isBlacklisted = isBlacklisted,
            categoryIcon = categoryIcon,
            typeColor = typeColor,
            typeBg = typeBg,
            onDismiss = { showDetailSheet = false },
            onChangeCategory = {
                showDetailSheet = false
                onAssignCategory?.invoke(expense)
            },
            onToggleBlacklist = {
                onToggleBlacklist?.invoke(expense.merchantOrRecipient)
                showDetailSheet = false
            },
            onToggleRecurring = {
                onToggleRecurring?.invoke(expense.id, !expense.isRecurring)
            },
            onOpenMerchantSheet = {
                showDetailSheet = false
                onOpenMerchantSheet?.invoke(expense.merchantOrRecipient)
            },
            onDelete = {
                onDelete(expense.id)
                showDetailSheet = false
            },
            onEditMerchant = onEditMerchant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailBottomSheet(
    expense: ExpenseEntity,
    formattedAmount: String,
    formattedDate: String,
    isBlacklisted: Boolean,
    categoryIcon: ImageVector,
    typeColor: Color,
    typeBg: Color,
    onDismiss: () -> Unit,
    onChangeCategory: () -> Unit,
    onToggleBlacklist: () -> Unit,
    onToggleRecurring: (() -> Unit)? = null,
    onOpenMerchantSheet: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onEditMerchant: ((id: Long, oldMerchant: String, newMerchant: String, category: String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current
    var copiedToClipboard by remember { mutableStateOf(false) }
    var showEditMerchantDialog by remember { mutableStateOf(false) }
    var editedMerchantName by remember(expense.merchantOrRecipient) { mutableStateOf(expense.merchantOrRecipient) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
            .testTag("transaction_details_bottom_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Transaction Details",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = SavioSlateDark
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SavioSlateMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Hero Banner Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isBlacklisted) SavioBlacklistBg else typeBg.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isBlacklisted) SavioBlacklistRed.copy(alpha = 0.3f) else typeColor.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isBlacklisted) SavioBlacklistBg else typeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBlacklisted) Icons.Default.Block else categoryIcon,
                            contentDescription = null,
                            tint = if (isBlacklisted) SavioBlacklistRed else typeColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = expense.merchantOrRecipient,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                textDecoration = if (isBlacklisted) TextDecoration.LineThrough else TextDecoration.None,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isBlacklisted) SavioBlacklistMuted else SavioSlateDark,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (onEditMerchant != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = SavioEmeraldContainer,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        editedMerchantName = expense.merchantOrRecipient
                                        showEditMerchantDialog = true
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Merchant Name",
                                        tint = SavioEmerald,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            textDecoration = if (isBlacklisted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isBlacklisted) SavioBlacklistMuted else typeColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Chips Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = typeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = expense.type.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = typeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SavioEmeraldContainer,
                            modifier = Modifier.clickable { onChangeCategory() }
                        ) {
                            Text(
                                text = "${expense.category} ✎",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SavioEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (isBlacklisted) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SavioBlacklistBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SavioBlacklistRed.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Excluded from Total",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SavioBlacklistRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Metadata Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GlassBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ModernDetailRow("Date & Time", formattedDate)
                    ModernDetailRow("Category", expense.category)
                    if (expense.accountInfo.isNotBlank()) {
                        ModernDetailRow("Payment Method", expense.accountInfo)
                    }
                    if (expense.sender.isNotBlank()) {
                        ModernDetailRow("SMS Origin", expense.sender)
                    }
                    ModernDetailRow("Transaction ID", "#${expense.id}")
                }
            }

            // Raw SMS Message Container
            if (expense.rawBody.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Original SMS Body",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(expense.rawBody))
                            copiedToClipboard = true
                        }
                    ) {
                        Icon(
                            imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy SMS",
                            tint = if (copiedToClipboard) SavioEmerald else SavioSlateMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (copiedToClipboard) "Copied!" else "Copy",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (copiedToClipboard) SavioEmerald else SavioSlateMuted
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GlassCardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = expense.rawBody,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier.padding(12.dp),
                        color = SavioSlateBody
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Merchant Intelligence & Recurring Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenMerchantSheet?.invoke() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = SavioSlateDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Vendor Analytics",
                        color = SavioSlateDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = { onToggleRecurring?.invoke() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (expense.isRecurring) Color(0xFF93C5FD) else GlassCardBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (expense.isRecurring) Color(0xFFEFF6FF) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.EventRepeat,
                        contentDescription = null,
                        tint = if (expense.isRecurring) Color(0xFF2563EB) else SavioSlateMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (expense.isRecurring) "Recurring" else "Mark Recurring",
                        color = if (expense.isRecurring) Color(0xFF2563EB) else SavioSlateDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onChangeCategory,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SavioEmerald)
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = SavioEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Category", color = SavioEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onToggleBlacklist,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBlacklisted) SavioEmerald else SavioBlacklistRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBlacklisted) "Unblacklist" else "Blacklist",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SavioSpendRose.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = SavioSpendRose
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        if (showEditMerchantDialog) {
            AlertDialog(
                onDismissRequest = { showEditMerchantDialog = false },
                title = {
                    Text(
                        text = "Edit Merchant Name",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Renaming this merchant will update this transaction, match past transactions, and save an automatic classification and alias rule for future SMS from '${expense.merchantOrRecipient}'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SavioSlateMuted
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = editedMerchantName,
                            onValueChange = { editedMerchantName = it },
                            label = { Text("Merchant Name") },
                            placeholder = { Text("e.g. Swiggy") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SavioEmerald,
                                unfocusedBorderColor = GlassCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editedMerchantName.isNotBlank()) {
                                onEditMerchant?.invoke(
                                    expense.id,
                                    expense.merchantOrRecipient,
                                    editedMerchantName.trim(),
                                    expense.category
                                )
                                showEditMerchantDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save & Apply Rule")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditMerchantDialog = false }) {
                        Text("Cancel", color = SavioSlateMuted)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White
            )
        }
    }
}

@Composable
private fun ModernDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SavioSlateMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = SavioSlateDark
        )
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Transfers" -> Icons.Default.SwapHoriz
        "Credit Card Bill" -> Icons.Default.CreditCard
        "Self" -> Icons.Default.AccountBalance
        "Groceries" -> Icons.Default.ShoppingCart
        "Shopping" -> Icons.Default.ShoppingBag
        "Food & Dining" -> Icons.Default.Fastfood
        "Bills & Utilities" -> Icons.Default.Receipt
        "Travel & Commute" -> Icons.Default.Flight
        else -> Icons.Default.CreditCard
    }
}
