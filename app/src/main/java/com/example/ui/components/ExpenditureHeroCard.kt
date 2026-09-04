package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import com.example.ui.theme.BentoCardBg
import com.example.ui.theme.BentoCardBorder
import com.example.ui.theme.BentoDebitRed
import com.example.ui.theme.BentoDebitRedBg
import com.example.ui.theme.BentoLavenderCard
import com.example.ui.theme.BentoLavenderContainer
import com.example.ui.theme.BentoPillDark
import com.example.ui.theme.BentoPurpleDark
import com.example.ui.theme.BentoPurplePrimary
import com.example.ui.theme.BentoSpendPlum
import com.example.ui.theme.BentoSpendPlumBg
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoTransferPurple
import com.example.ui.theme.BentoTransferPurpleBg
import com.example.ui.theme.SavioSavingsGreen
import com.example.ui.theme.SavioSavingsGreenBg
import com.example.ui.theme.SavioSpendRed
import com.example.ui.theme.StatusActiveGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ExpenditureHeroCard(
    monthDisplay: String,
    totalExpenditure: Double,
    debitsTotal: Double,
    transfersTotal: Double,
    spendsTotal: Double,
    currency: String,
    monthlySalary: Double = 0.0,
    monthlySavings: Double = 0.0,
    savingsGoal: Double = 0.0,
    monthlyBudget: Double = 0.0,
    isNotificationActive: Boolean,
    onToggleNotification: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    val totalFormatted = "$currency${numberFormatter.format(totalExpenditure)}"
    val debitsFormatted = "$currency${numberFormatter.format(debitsTotal)}"
    val transfersFormatted = "$currency${numberFormatter.format(transfersTotal)}"
    val spendsFormatted = "$currency${numberFormatter.format(spendsTotal)}"
    val salaryFormatted = "$currency${numberFormatter.format(monthlySalary)}"
    val savingsFormatted = if (monthlySavings >= 0) {
        "+$currency${numberFormatter.format(monthlySavings)}"
    } else {
        "-$currency${numberFormatter.format(-monthlySavings)}"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_expenditure_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BentoLavenderCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top row: Month & Status bar sync badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = monthDisplay.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = BentoPurpleDark
                    )
                    Text(
                        text = "Total Monthly Expenditure",
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoTextSecondary
                    )
                }

                // Status Bar Notification Pill Indicator (Bento style)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BentoPurpleDark,
                    modifier = Modifier.clickable { onToggleNotification(!isNotificationActive) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isNotificationActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(pulseAlpha)
                                    .clip(CircleShape)
                                    .background(StatusActiveGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Status Bar Live",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "Notification Off",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Status Bar Off",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF87171)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bento Main Large Amount Display
            Text(
                text = totalFormatted,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.0).sp
                ),
                color = BentoPurpleDark
            )

            // Bento Salary & Live Savings Summary Card
            if (monthlySalary > 0) {
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Salary",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = BentoTextSecondary
                                )
                                Text(
                                    text = salaryFormatted,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BentoPurpleDark
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (monthlySavings >= 0) "Amount Saved Live" else "Overspent Deficit",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = BentoTextSecondary
                                )
                                Text(
                                    text = savingsFormatted,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (monthlySavings >= 0) SavioSavingsGreen else BentoDebitRed
                                )
                            }
                        }

                        // Savings Goal Progress Bar
                        if (savingsGoal > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val goalProgress = (monthlySavings / savingsGoal).toFloat().coerceIn(0f, 1f)
                            val goalPercent = ((monthlySavings / savingsGoal) * 100).toInt()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Goal: $currency${numberFormatter.format(savingsGoal)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = BentoTextSecondary
                                )
                                Text(
                                    text = if (monthlySavings >= savingsGoal) "Goal Achieved! ($goalPercent%)" else "$goalPercent% achieved",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (monthlySavings >= savingsGoal) SavioSavingsGreen else BentoPurplePrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LinearProgressIndicator(
                                progress = { goalProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (monthlySavings >= savingsGoal) SavioSavingsGreen else BentoPurplePrimary,
                                trackColor = BentoLavenderContainer
                            )
                        }
                    }
                }
            }

            // Budget indicator if set
            if (monthlyBudget > 0) {
                val progress = (totalExpenditure / monthlyBudget).toFloat().coerceIn(0f, 1f)
                val percentInt = ((totalExpenditure / monthlyBudget) * 100).toInt()
                val remaining = (monthlyBudget - totalExpenditure).coerceAtLeast(0.0)

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Budget: $currency${numberFormatter.format(monthlyBudget)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = BentoPurpleDark
                        )
                        Text(
                            text = if (totalExpenditure > monthlyBudget) {
                                "Exceeded by $currency${numberFormatter.format(totalExpenditure - monthlyBudget)}"
                            } else {
                                "$currency${numberFormatter.format(remaining)} left ($percentInt%)"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (totalExpenditure > monthlyBudget) BentoDebitRed else BentoPurplePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (totalExpenditure > monthlyBudget) BentoDebitRed else BentoPurplePrimary,
                        trackColor = BentoLavenderContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bento Grid Breakdown Tiles: Debits, Transfers, Spends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpendCategoryPill(
                    label = "Debits",
                    amount = debitsFormatted,
                    icon = Icons.Default.ArrowDownward,
                    accentColor = BentoDebitRed,
                    backgroundColor = BentoDebitRedBg,
                    modifier = Modifier.weight(1f)
                )

                SpendCategoryPill(
                    label = "Transfers",
                    amount = transfersFormatted,
                    icon = Icons.Default.SwapHoriz,
                    accentColor = BentoTransferPurple,
                    backgroundColor = BentoTransferPurpleBg,
                    modifier = Modifier.weight(1f)
                )

                SpendCategoryPill(
                    label = "Spends",
                    amount = spendsFormatted,
                    icon = Icons.Default.CreditCard,
                    accentColor = BentoSpendPlum,
                    backgroundColor = BentoSpendPlumBg,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SpendCategoryPill(
    label: String,
    amount: String,
    icon: ImageVector,
    accentColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                ),
                color = BentoTextPrimary,
                maxLines = 1
            )
        }
    }
}
