package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Category
import com.example.sms.SmsParser
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
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

private data class SwipeActionStyle(
    val bgColor: Color,
    val icon: ImageVector,
    val text: String,
    val tint: Color
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionItemCard(
    expense: ExpenseEntity,
    currency: String = expense.currency,
    isBlacklisted: Boolean = false,
    onDelete: (Long) -> Unit,
    onAssignCategory: ((ExpenseEntity) -> Unit)? = null,
    onToggleBlacklist: ((String) -> Unit)? = null,
    onToggleExclude: ((Long, Boolean) -> Unit)? = null,
    onToggleRecurring: ((Long, Boolean) -> Unit)? = null,
    onOpenMerchantSheet: ((String) -> Unit)? = null,
    onEditMerchant: ((id: Long, oldMerchant: String, newMerchant: String, category: String) -> Unit)? = null,
    onUpdateRefundSettlement: ((Long, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDetailSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRefundSettlementDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current
    var actionRowWidthPx by remember { mutableStateOf(0f) }
    val horizontalPaddingPx = with(density) { 20.dp.toPx() }
    val extraSlackPx = with(density) { 20.dp.toPx() }
    val defaultMaxSlidePx = with(density) { 165.dp.toPx() }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { with(density) { 72.dp.toPx() } },
        confirmValueChange = { targetValue ->
            when (targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleRecurring?.invoke(expense.id, !expense.isRecurring)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleExclude?.invoke(expense.id, !expense.isExcluded)
                    false
                }
                else -> false
            }
        }
    )

    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val effectiveCurrency = if (currency.isNotBlank()) currency else expense.currency
    val isCreditReversal = expense.isRefundOrReversal
    val formattedAmount = if (isCreditReversal) {
        "+${effectiveCurrency}${numberFormatter.format(expense.amount)}"
    } else {
        "-${effectiveCurrency}${numberFormatter.format(expense.amount)}"
    }

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

    val (typeColor, typeBg, _) = if (isCreditReversal) {
        Triple(SavioEmerald, SavioEmeraldContainer, Icons.Default.CheckCircle)
    } else when (expense.type) {
        ExpenseType.P2P -> Triple(SavioTransferIndigo, SavioTransferIndigoBg, Icons.Default.SwapHoriz)
        ExpenseType.SELF -> Triple(SavioEmerald, SavioEmeraldContainer, Icons.Default.AccountBalance)
        ExpenseType.CREDIT_CARD -> Triple(Color(0xFF7C3AED), Color(0xFFF5F3FF), Icons.Default.CreditCard)
        ExpenseType.MERCHANT -> Triple(SavioSpendRose, SavioSpendRoseBg, Icons.Default.CreditCard)
    }

    val categoryIcon = getCategoryIcon(expense.category)

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = onToggleRecurring != null,
        enableDismissFromEndToStart = onToggleExclude != null,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val style = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (expense.isRecurring) {
                        SwipeActionStyle(
                            bgColor = Color(0xFFFEF2F2),
                            icon = Icons.Default.EventBusy,
                            text = "Unmark Recurring",
                            tint = SavioSpendRose
                        )
                    } else {
                        SwipeActionStyle(
                            bgColor = Color(0xFFEFF6FF),
                            icon = Icons.Default.EventRepeat,
                            text = "Mark Recurring",
                            tint = Color(0xFF2563EB)
                        )
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (expense.isExcluded) {
                        SwipeActionStyle(
                            bgColor = SavioEmeraldContainer,
                            icon = Icons.Default.CheckCircle,
                            text = "Include in Spend",
                            tint = SavioEmerald
                        )
                    } else {
                        SwipeActionStyle(
                            bgColor = SavioBlacklistBg,
                            icon = Icons.Default.Block,
                            text = "Exclude from Spend",
                            tint = SavioBlacklistRed
                        )
                    }
                }
                else -> SwipeActionStyle(
                    bgColor = Color.Transparent,
                    icon = Icons.Default.Check,
                    text = "",
                    tint = Color.Transparent
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(style.bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (direction != SwipeToDismissBoxValue.Settled) {
                    Row(
                        modifier = Modifier.onSizeChanged { size ->
                            if (size.width > 0) {
                                actionRowWidthPx = size.width.toFloat()
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (alignment == Alignment.CenterStart) {
                            Icon(
                                imageVector = style.icon,
                                contentDescription = null,
                                tint = style.tint,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = style.text,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = style.tint
                            )
                        } else {
                            Text(
                                text = style.text,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = style.tint
                            )
                            Icon(
                                imageVector = style.icon,
                                contentDescription = null,
                                tint = style.tint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("expense_card_${expense.id}")
                .graphicsLayer {
                    val rawOffset = try {
                        dismissState.requireOffset()
                    } catch (e: Exception) {
                        0f
                    }
                    val maxAllowed = if (actionRowWidthPx > 0f) {
                        actionRowWidthPx + horizontalPaddingPx + extraSlackPx
                    } else {
                        defaultMaxSlidePx
                    }
                    val desiredOffset = if (rawOffset > 0f) {
                        if (rawOffset <= maxAllowed) rawOffset else maxAllowed + (rawOffset - maxAllowed) * 0.12f
                    } else if (rawOffset < 0f) {
                        if (rawOffset >= -maxAllowed) rawOffset else -maxAllowed + (rawOffset - (-maxAllowed)) * 0.12f
                    } else {
                        0f
                    }
                    translationX = desiredOffset - rawOffset
                }
                .clip(RoundedCornerShape(18.dp))
                .combinedClickable(
                    onClick = { showDetailSheet = true },
                    onDoubleClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAssignCategory?.invoke(expense)
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteConfirmDialog = true
                    }
                ),
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
            val isIgnored = expense.isExcluded || isBlacklisted

            // Category / Type Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isIgnored) SavioBlacklistBg else typeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIgnored) Icons.Default.Block else categoryIcon,
                    contentDescription = expense.category,
                    tint = if (isIgnored) SavioBlacklistRed else typeColor,
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
                            textDecoration = if (isIgnored) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isIgnored) SavioBlacklistMuted else SavioSlateDark,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (expense.isExcluded) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SavioBlacklistBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SavioBlacklistRed.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Excluded",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = SavioBlacklistRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isBlacklisted) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SavioBlacklistBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SavioBlacklistRed.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Blacklisted",
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
                        color = SavioEmerald,
                        modifier = Modifier.clickable { showRefundSettlementDialog = true }
                    )
                } else if (expense.isPartiallyRefunded) {
                    Text(
                        text = "-${effectiveCurrency}${numberFormatter.format(expense.netAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textDecoration = if (isIgnored) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isIgnored) SavioBlacklistMuted else typeColor
                    )
                    Text(
                        text = "(${effectiveCurrency}${numberFormatter.format(expense.refundedAmount)} refunded)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = SavioEmerald,
                        modifier = Modifier.clickable { showRefundSettlementDialog = true }
                    )
                } else {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textDecoration = if (isIgnored) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isIgnored) SavioBlacklistMuted else typeColor
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = expense.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = if (expense.category.equals("Uncategorized", ignoreCase = true)) {
                                    SavioSpendRose
                                } else {
                                    SavioEmerald
                                },
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Category",
                                tint = if (expense.category.equals("Uncategorized", ignoreCase = true)) {
                                    SavioSpendRose
                                } else {
                                    SavioEmerald
                                },
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }

                    if (expense.isRecurring) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.clickable {
                                onToggleRecurring?.invoke(expense.id, !expense.isRecurring)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventRepeat,
                                    contentDescription = "Bill",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Bill",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFF2563EB),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = SavioSpendRose,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Transaction?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SavioSlateDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to permanently delete this transaction record?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SavioSlateBody
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SavioSpendRoseBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = expense.merchantOrRecipient,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SavioSlateDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SavioSlateMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formattedAmount,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = SavioSpendRose
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(expense.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SavioSpendRose)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = SavioSlateDark)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
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
            onToggleExclude = {
                onToggleExclude?.invoke(expense.id, !expense.isExcluded)
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
            onEditMerchant = onEditMerchant,
            onOpenRefundSettlementDialog = {
                showRefundSettlementDialog = true
            }
        )
    }

    if (showRefundSettlementDialog) {
        RefundSettlementDialog(
            expense = expense,
            currency = effectiveCurrency,
            onDismiss = { showRefundSettlementDialog = false },
            onConfirmSettlement = { amount ->
                onUpdateRefundSettlement?.invoke(expense.id, amount)
            }
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
    onToggleExclude: (() -> Unit)? = null,
    onToggleRecurring: (() -> Unit)? = null,
    onOpenMerchantSheet: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onEditMerchant: ((id: Long, oldMerchant: String, newMerchant: String, category: String) -> Unit)? = null,
    onOpenRefundSettlementDialog: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current
    var copiedToClipboard by remember { mutableStateOf(false) }
    var showEditMerchantDialog by remember { mutableStateOf(false) }
    var editedMerchantName by remember(expense.merchantOrRecipient) { mutableStateOf(expense.merchantOrRecipient) }
    var isRecurringState by remember(expense.id, expense.isRecurring) { mutableStateOf(expense.isRecurring) }
    val originalParsedMerchant = remember(expense.rawBody) {
        if (expense.rawBody.isNotBlank()) {
            SmsParser.parse(expense.rawBody)?.title
        } else null
    }
    val canUndoMerchant = originalParsedMerchant != null &&
        originalParsedMerchant.isNotBlank() &&
        !originalParsedMerchant.equals(expense.merchantOrRecipient, ignoreCase = true)

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

            val isIgnored = expense.isExcluded || isBlacklisted

            // Main Hero Banner Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isIgnored) SavioBlacklistBg else typeBg.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isIgnored) SavioBlacklistRed.copy(alpha = 0.3f) else typeColor.copy(alpha = 0.2f)
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
                            .background(if (isIgnored) SavioBlacklistBg else typeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isIgnored) Icons.Default.Block else categoryIcon,
                            contentDescription = null,
                            tint = if (isIgnored) SavioBlacklistRed else typeColor,
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
                                textDecoration = if (isIgnored) TextDecoration.LineThrough else TextDecoration.None,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isIgnored) SavioBlacklistMuted else SavioSlateDark,
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

                            if (canUndoMerchant) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val targetName = originalParsedMerchant!!
                                            editedMerchantName = targetName
                                            onEditMerchant(expense.id, expense.merchantOrRecipient, targetName, expense.category)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = "Undo to Original Merchant Name ($originalParsedMerchant)",
                                            tint = SavioSlateDark,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
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
                            textDecoration = if (isIgnored) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isIgnored) SavioBlacklistMuted else typeColor
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = expense.category,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SavioEmerald
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Category",
                                    tint = SavioEmerald,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        if (expense.isExcluded) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SavioBlacklistBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SavioBlacklistRed.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Excluded from Spend",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SavioBlacklistRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else if (isBlacklisted) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SavioBlacklistBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SavioBlacklistRed.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Blacklisted Merchant",
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
                    onClick = {
                        isRecurringState = !isRecurringState
                        onToggleRecurring?.invoke()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRecurringState) Color(0xFF93C5FD) else GlassCardBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isRecurringState) Color(0xFFEFF6FF) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.EventRepeat,
                        contentDescription = null,
                        tint = if (isRecurringState) Color(0xFF2563EB) else SavioSlateMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecurringState) "Recurring" else "Mark Recurring",
                        color = if (isRecurringState) Color(0xFF2563EB) else SavioSlateDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Refund / Reimbursement / Split Pay Settlement Button
            OutlinedButton(
                onClick = onOpenRefundSettlementDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (expense.refundedAmount > 0) SavioEmerald else GlassCardBorder
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (expense.refundedAmount > 0) SavioEmeraldContainer else Color.Transparent
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CallSplit,
                    contentDescription = null,
                    tint = if (expense.refundedAmount > 0) SavioEmerald else SavioSlateDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (expense.isFullyRefunded) "Refunded / Settled (100% Excluded - Edit)"
                           else if (expense.isPartiallyRefunded) "Settled: -${expense.currency}${NumberFormat.getNumberInstance(Locale.US).format(expense.refundedAmount)} (Edit)"
                           else "Log Refund / Split Settlement",
                    color = if (expense.refundedAmount > 0) SavioEmerald else SavioSlateDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                )
            }

            if (onToggleExclude != null) {
                OutlinedButton(
                    onClick = onToggleExclude,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (expense.isExcluded) SavioEmerald else SavioBlacklistRed.copy(alpha = 0.5f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (expense.isExcluded) SavioEmeraldContainer else SavioBlacklistBg
                    )
                ) {
                    Icon(
                        imageVector = if (expense.isExcluded) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = null,
                        tint = if (expense.isExcluded) SavioEmerald else SavioBlacklistRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (expense.isExcluded) "Include Transaction in Spend" else "Exclude this Transaction from Spend",
                        color = if (expense.isExcluded) SavioEmerald else SavioBlacklistRed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
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
                        text = if (isBlacklisted) "Unblacklist Merchant" else "Blacklist Merchant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
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
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (canUndoMerchant) {
                            TextButton(
                                onClick = {
                                    editedMerchantName = originalParsedMerchant!!
                                }
                            ) {
                                Text("Reset to Original", color = SavioEmerald)
                            }
                        }
                        TextButton(onClick = { showEditMerchantDialog = false }) {
                            Text("Cancel", color = SavioSlateMuted)
                        }
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

@Composable
fun RefundSettlementDialog(
    expense: ExpenseEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirmSettlement: (Double) -> Unit
) {
    val effectiveCurrency = if (currency.isNotBlank()) currency else expense.currency
    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    var settlementInput by remember(expense.refundedAmount) {
        mutableStateOf(
            if (expense.refundedAmount > 0.0) {
                String.format(Locale.US, "%.2f", expense.refundedAmount)
            } else ""
        )
    }

    val parsedSettlement = settlementInput.toDoubleOrNull() ?: 0.0
    val netRemaining = (expense.amount - parsedSettlement).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SavioEmeraldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CallSplit,
                    contentDescription = null,
                    tint = SavioEmerald,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Log Settlement / Refund",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SavioSlateDark,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Exclude refunds, company reimbursements, or friend splits from your spend totals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SavioSlateMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Original Transaction Reference Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GlassCardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = expense.merchantOrRecipient,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SavioSlateDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Original Transaction Spend",
                                style = MaterialTheme.typography.labelSmall,
                                color = SavioSlateMuted
                            )
                        }
                        Text(
                            text = "$effectiveCurrency${numberFormatter.format(expense.amount)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = SavioSlateDark
                        )
                    }
                }

                // Settlement Amount Input
                Column {
                    Text(
                        text = "Settlement / Refund Amount",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = settlementInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) {
                                settlementInput = filtered
                            }
                        },
                        placeholder = { Text("0.00") },
                        prefix = {
                            Text(
                                text = "$effectiveCurrency ",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = SavioEmerald
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SavioEmerald,
                            unfocusedBorderColor = GlassCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quick Preset Chips (1-tap settlement selection)
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SavioSlateMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Full (100%)" to expense.amount,
                        "50% (½)" to expense.amount / 2.0,
                        "33% (⅓)" to expense.amount / 3.0,
                        "25% (¼)" to expense.amount / 4.0
                    ).forEach { (label, amt) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GlassBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    settlementInput = String.format(Locale.US, "%.2f", amt)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = SavioEmerald,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "$effectiveCurrency${numberFormatter.format(amt)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = SavioSlateMuted,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Live Impact Preview Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (parsedSettlement > 0.0) SavioEmeraldContainer.copy(alpha = 0.5f) else GlassBackground,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (parsedSettlement > 0.0) SavioEmeraldBorder else GlassCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Excluded from Spend:",
                                style = MaterialTheme.typography.bodySmall,
                                color = SavioSlateDark
                            )
                            Text(
                                text = "-$effectiveCurrency${numberFormatter.format(parsedSettlement)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = SavioEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Net Spend Counted:",
                                style = MaterialTheme.typography.bodySmall,
                                color = SavioSlateDark
                            )
                            Text(
                                text = "$effectiveCurrency${numberFormatter.format(netRemaining)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (netRemaining == 0.0) SavioEmerald else SavioSlateDark
                            )
                        }
                        if (parsedSettlement >= expense.amount && expense.amount > 0.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "✓ 100% of this transaction is reversed and will not count towards your monthly spend.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = SavioEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmSettlement(parsedSettlement)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SavioEmerald),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Apply Settlement",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (expense.refundedAmount > 0.0) {
                    TextButton(
                        onClick = {
                            onConfirmSettlement(0.0)
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = "Clear",
                            color = SavioSpendRose,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = SavioSlateDark)
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp)
    )
}

