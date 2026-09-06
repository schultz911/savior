package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AmountRange
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldBorder
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSlateBody
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import com.example.ui.theme.SavioSpendRose
import com.example.ui.theme.SavioSpendRoseBg

@Composable
fun SearchFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchExpanded: Boolean,
    onToggleSearchExpanded: (Boolean) -> Unit,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    availableCategories: List<String>,
    selectedAmountRange: AmountRange,
    onSelectAmountRange: (AmountRange) -> Unit,
    onlyRecurring: Boolean,
    onToggleOnlyRecurring: (Boolean) -> Unit,
    isGlobalSearch: Boolean = false,
    onToggleGlobalSearch: (Boolean) -> Unit = {},
    onClearAllFilters: () -> Unit,
    currency: String,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val hasActiveFilters = searchQuery.isNotBlank() ||
            selectedCategory != null ||
            selectedAmountRange != AmountRange.ALL ||
            onlyRecurring ||
            isGlobalSearch

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_filter_bar")
    ) {
        // Search Input Bar
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = GlassCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (searchQuery.isNotBlank()) SavioEmerald else SavioSlateMuted,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = if (isGlobalSearch) "Search across all historical months..." else "Search merchant, amount, category...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SavioSlateMuted
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = SavioSlateDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(SavioEmerald),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_query_input")
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Search",
                            tint = SavioSlateMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isGlobalSearch) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SavioEmeraldContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SavioEmeraldBorder),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "🌐 All Time",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = SavioEmerald
                        )
                    }
                }

                // Filter expand/collapse button with indicator dot
                Box {
                    IconButton(
                        onClick = { onToggleSearchExpanded(!isSearchExpanded) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSearchExpanded || hasActiveFilters) SavioEmeraldContainer else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filters",
                            tint = if (isSearchExpanded || hasActiveFilters) SavioEmerald else SavioSlateDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (hasActiveFilters && !isSearchExpanded) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SavioEmerald)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }

        // Expandable Filter Section (Amount Brackets, Recurring, Categories)
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                // Header row: Filter Pills & Reset All Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FILTER BY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = SavioSlateMuted
                    )

                    if (hasActiveFilters) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onClearAllFilters() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterListOff,
                                contentDescription = null,
                                tint = SavioSpendRose,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset Filters",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SavioSpendRose
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Amount Brackets & Recurring Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Global Search Scope Pill
                    FilterChip(
                        label = if (isGlobalSearch) "🌐 All Months (Global)" else "📅 This Month",
                        isSelected = isGlobalSearch,
                        onClick = { onToggleGlobalSearch(!isGlobalSearch) }
                    )

                    // Recurring Only Pill
                    FilterChip(
                        label = "🔁 Recurring Only",
                        isSelected = onlyRecurring,
                        onClick = { onToggleOnlyRecurring(!onlyRecurring) }
                    )

                    // Amount Brackets
                    FilterChip(
                        label = "All Amounts",
                        isSelected = selectedAmountRange == AmountRange.ALL,
                        onClick = { onSelectAmountRange(AmountRange.ALL) }
                    )

                    FilterChip(
                        label = "< $currency" + "500",
                        isSelected = selectedAmountRange == AmountRange.UNDER_500,
                        onClick = { onSelectAmountRange(AmountRange.UNDER_500) }
                    )

                    FilterChip(
                        label = "$currency" + "500 - $currency" + "2K",
                        isSelected = selectedAmountRange == AmountRange.MID_500_2000,
                        onClick = { onSelectAmountRange(AmountRange.MID_500_2000) }
                    )

                    FilterChip(
                        label = "> $currency" + "2K",
                        isSelected = selectedAmountRange == AmountRange.OVER_2000,
                        onClick = { onSelectAmountRange(AmountRange.OVER_2000) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Categories Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        label = "All Categories",
                        isSelected = selectedCategory == null,
                        onClick = { onSelectCategory(null) }
                    )

                    for (cat in availableCategories) {
                        FilterChip(
                            label = cat,
                            isSelected = selectedCategory.equals(cat, ignoreCase = true),
                            onClick = {
                                if (selectedCategory.equals(cat, ignoreCase = true)) {
                                    onSelectCategory(null)
                                } else {
                                    onSelectCategory(cat)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) SavioEmeraldContainer else Color.White.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) SavioEmeraldBorder else GlassCardBorder
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = if (isSelected) SavioEmerald else SavioSlateBody
        )
    }
}
