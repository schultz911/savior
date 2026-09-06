package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.models.DailyBurnDownData
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.SavioEmeraldBorder
import com.example.ui.theme.SavioEmeraldContainer
import com.example.ui.theme.SavioSlateBody
import com.example.ui.theme.SavioSlateDark
import com.example.ui.theme.SavioSlateMuted
import com.example.ui.theme.SavioSlateSubtle
import com.example.ui.theme.SavioSpendRose
import com.example.ui.theme.SavioSpendRoseBg
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DailyBurnDownChart(
    burnDownData: DailyBurnDownData,
    currency: String,
    modifier: Modifier = Modifier
) {
    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }

    val isOverPaced = burnDownData.isOverPaced
    val primaryColor = if (isOverPaced) SavioSpendRose else SavioEmerald
    val containerColor = if (isOverPaced) SavioSpendRoseBg else SavioEmeraldContainer
    val budget = burnDownData.monthlyBudget
    val currentSpend = burnDownData.currentCumulativeSpend

    val progressAnim = animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "burndown_anim"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCardBg),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
            .testTag("daily_burndown_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Spend Velocity",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = SavioSlateDark
                        )
                        Text(
                            text = "Day ${burnDownData.currentDay} of ${burnDownData.daysInMonth} pacing",
                            style = MaterialTheme.typography.bodySmall,
                            color = SavioSlateMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = containerColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isOverPaced) SavioSpendRose.copy(alpha = 0.3f) else SavioEmeraldBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOverPaced) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOverPaced) "Over-Paced" else "On Track",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = primaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Key Metrics Pill Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassBackground)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current Spend",
                        style = MaterialTheme.typography.labelSmall,
                        color = SavioSlateMuted
                    )
                    Text(
                        text = "$currency${numberFormatter.format(currentSpend)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SavioSlateDark
                    )
                }

                Column {
                    Text(
                        text = "Burn Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = SavioSlateMuted
                    )
                    Text(
                        text = "$currency${numberFormatter.format(burnDownData.currentBurnRatePerDay)}/day",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = primaryColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Projected End",
                        style = MaterialTheme.typography.labelSmall,
                        color = SavioSlateMuted
                    )
                    Text(
                        text = "$currency${numberFormatter.format(burnDownData.projectedMonthEndSpend)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (budget > 0 && burnDownData.projectedMonthEndSpend > budget) SavioSpendRose else SavioSlateDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Line Chart
            val points = burnDownData.points
            val daysInMonth = burnDownData.daysInMonth.coerceAtLeast(28)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val bottomPadding = 20f
                    val topPadding = 12f
                    val chartHeight = h - bottomPadding - topPadding

                    val maxVal = maxOf(
                        budget.toFloat(),
                        burnDownData.projectedMonthEndSpend.toFloat(),
                        (points.lastOrNull()?.cumulativeSpend?.toFloat() ?: 0f) * 1.1f,
                        1000f
                    )

                    fun xCoord(day: Int): Float = ((day - 1).toFloat() / (daysInMonth - 1).toFloat()) * w
                    fun yCoord(amount: Float): Float = topPadding + chartHeight * (1f - (amount / maxVal).coerceIn(0f, 1f))

                    // 1. Target Budget Reference Line (Dashed)
                    val targetY = yCoord(budget.toFloat())
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    // Draw budget guideline
                    if (budget > 0) {
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.8f),
                            start = Offset(0f, targetY),
                            end = Offset(w, targetY),
                            strokeWidth = 2f,
                            pathEffect = dashEffect
                        )

                        // Ideal Linear Burn Pacing Line from 0 to Budget
                        val linearPacingPath = Path().apply {
                            moveTo(xCoord(1), yCoord(0f))
                            lineTo(xCoord(daysInMonth), targetY)
                        }
                        drawPath(
                            path = linearPacingPath,
                            color = SavioSlateMuted.copy(alpha = 0.35f),
                            style = Stroke(width = 2.5f, pathEffect = dashEffect)
                        )
                    }

                    // 2. Actual Cumulative Spend Curve
                    if (points.isNotEmpty()) {
                        val curvePath = Path()
                        val fillPath = Path()

                        val startX = xCoord(points.first().dayOfMonth)
                        val startY = yCoord(points.first().cumulativeSpend.toFloat() * progressAnim.value)

                        curvePath.moveTo(startX, startY)
                        fillPath.moveTo(startX, yCoord(0f))
                        fillPath.lineTo(startX, startY)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]

                            val x0 = xCoord(p0.dayOfMonth)
                            val y0 = yCoord(p0.cumulativeSpend.toFloat() * progressAnim.value)
                            val x1 = xCoord(p1.dayOfMonth)
                            val y1 = yCoord(p1.cumulativeSpend.toFloat() * progressAnim.value)

                            val cx = (x0 + x1) / 2f
                            curvePath.cubicTo(cx, y0, cx, y1, x1, y1)
                            fillPath.cubicTo(cx, y0, cx, y1, x1, y1)
                        }

                        val lastPoint = points.last()
                        val lastX = xCoord(lastPoint.dayOfMonth)
                        val lastY = yCoord(lastPoint.cumulativeSpend.toFloat() * progressAnim.value)

                        fillPath.lineTo(lastX, yCoord(0f))
                        fillPath.close()

                        // Gradient fill under the actual curve
                        val gradientBrush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.28f),
                                primaryColor.copy(alpha = 0.02f)
                            ),
                            startY = topPadding,
                            endY = h - bottomPadding
                        )
                        drawPath(path = fillPath, brush = gradientBrush)

                        // Stroke the actual curve
                        drawPath(
                            path = curvePath,
                            color = primaryColor,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )

                        // Active Day Indicator Dot
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(lastX, lastY)
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 4.dp.toPx(),
                            center = Offset(lastX, lastY)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Actual Net Spend",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = SavioSlateDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .background(SavioSlateMuted)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Linear Benchmark (${currency}${numberFormatter.format(budget)})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = SavioSlateMuted
                    )
                }
            }
        }
    }
}
