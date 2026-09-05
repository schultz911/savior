package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseEntity
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSavingsGreen
import com.example.ui.theme.SavioSlateBody
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import com.example.ui.theme.SavioSpendRose
import com.example.ui.theme.SavioSpendRoseBg
import java.text.NumberFormat
import java.util.Locale

val PiePalette = listOf(
    Color(0xFF059669), // Emerald
    Color(0xFF4F46E5), // Indigo
    Color(0xFFE11D48), // Rose
    Color(0xFFD97706), // Amber
    Color(0xFF0891B2), // Cyan
    Color(0xFF7C3AED), // Violet
    Color(0xFFEA580C), // Orange
    Color(0xFF0D9488), // Teal
    Color(0xFFDB2777), // Pink
    Color(0xFF64748B)  // Slate
)

data class CategorySpendSlice(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color,
    val limit: Double = 0.0
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpendBreakupPieChartCard(
    expenses: List<ExpenseEntity>,
    currency: String,
    categoryLimits: Map<String, Double> = emptyMap(),
    blacklistedMerchants: Set<String> = emptySet(),
    onCategoryClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Exclude blacklisted merchants from spend breakup
    val validExpenses = remember(expenses, blacklistedMerchants) {
        expenses.filterNot { exp ->
            val norm = exp.merchantOrRecipient.trim()
            norm.isNotBlank() && blacklistedMerchants.any { norm.contains(it, ignoreCase = true) || it.contains(norm, ignoreCase = true) }
        }
    }

    if (validExpenses.isEmpty()) return

    val totalSpent = validExpenses.sumOf { it.amount }
    if (totalSpent <= 0.0) return

    // Group expenses by category
    val grouped = validExpenses.groupBy { it.category.ifBlank { "Uncategorized" } }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val slices = remember(grouped, totalSpent, categoryLimits) {
        grouped.mapIndexed { index, (cat, amt) ->
            val color = if (cat.equals("Uncategorized", ignoreCase = true)) {
                Color(0xFF94A3B8)
            } else {
                PiePalette[index % PiePalette.size]
            }
            val pct = ((amt / totalSpent) * 100f).toFloat()
            val limit = categoryLimits[cat] ?: 0.0
            CategorySpendSlice(
                category = cat,
                amount = amt,
                percentage = pct,
                color = color,
                limit = limit
            )
        }
    }

    var selectedSlice by remember { mutableStateOf<CategorySpendSlice?>(null) }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(validExpenses) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spend_breakup_pie_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = SavioEmeraldContainer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = SavioEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Spend Breakup",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SavioSlateDark
                        )
                    )
                }

                Text(
                    text = "$currency${numberFormatter.format(totalSpent)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = SavioEmerald
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Donut Pie Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(190.dp)
                        .testTag("pie_chart_canvas")
                ) {
                    val strokeWidth = 36.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    var startAngle = -90f

                    for (slice in slices) {
                        val sweep = (slice.percentage / 100f) * 360f * animatedProgress.value
                        val isCurrentSelected = selectedSlice?.category == slice.category

                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isCurrentSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                        startAngle += sweep
                    }
                }

                // Center Label
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val active = selectedSlice ?: slices.firstOrNull()
                    if (active != null) {
                        Text(
                            text = active.category,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = SavioSlateDark,
                            maxLines = 1
                        )
                        Text(
                            text = "${active.percentage.toInt()}%",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            ),
                            color = active.color
                        )
                        Text(
                            text = "$currency${numberFormatter.format(active.amount)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = SavioSlateMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Breakup Chips with Limit Indicators
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.forEach { slice ->
                    val isSelected = selectedSlice?.category == slice.category
                    val limit = slice.limit
                    val hasLimit = limit > 0.0
                    val limitRatio = if (hasLimit) (slice.amount / limit).toFloat() else 0f
                    val isNearLimit = hasLimit && limitRatio >= 0.8f && limitRatio < 1.0f
                    val isOverLimit = hasLimit && limitRatio >= 1.0f

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) slice.color.copy(alpha = 0.12f) else GlassBackground,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) slice.color else GlassCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSlice = if (isSelected) null else slice
                                onCategoryClick?.invoke(slice.category)
                            }
                            .testTag("category_slice_${slice.category}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(slice.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = slice.category,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = SavioSlateDark
                                    )
                                    if (slice.category == "Uncategorized") {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "(tap to set)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = SavioSpendRose
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$currency${numberFormatter.format(slice.amount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = SavioSlateDark
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${slice.percentage.toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = slice.color
                                    )
                                }
                            }

                            // Category limit progress
                            if (hasLimit) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LinearProgressIndicator(
                                        progress = { limitRatio.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = when {
                                            isOverLimit -> SavioSpendRose
                                            isNearLimit -> Color(0xFFF59E0B)
                                            else -> SavioSavingsGreen
                                        },
                                        trackColor = SavioEmeraldContainer
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = when {
                                            isOverLimit -> "⚠️ Overshot limit ($currency${numberFormatter.format(limit)})"
                                            isNearLimit -> "⚡ 80% limit warning ($currency${numberFormatter.format(limit)})"
                                            else -> "Limit: $currency${numberFormatter.format(limit)}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (isOverLimit || isNearLimit) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when {
                                            isOverLimit -> SavioSpendRose
                                            isNearLimit -> Color(0xFFB45309)
                                            else -> SavioSlateMuted
                                        }
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
