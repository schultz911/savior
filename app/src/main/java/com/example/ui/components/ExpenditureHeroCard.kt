package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.SavioBlacklistBg
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
import com.example.ui.theme.StatusActiveGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ExpenditureHeroCard(
    monthDisplay: String,
    totalExpenditure: Double,
    transfersTotal: Double,
    spendsTotal: Double,
    creditCardsTotal: Double = 0.0,
    selfTotal: Double = 0.0,
    currency: String,
    monthlySalary: Double = 0.0,
    monthlySavings: Double = 0.0,
    savingsGoal: Double = 0.0,
    monthlyBudget: Double = 0.0,
    debitsTotal: Double = 0.0,
    isNotificationActive: Boolean,
    onToggleNotification: (Boolean) -> Unit,
    safeSpendPacing: com.example.ui.SafeSpendPacing? = null,
    upcomingCommitmentsCount: Int = 0,
    onUpcomingCommitmentsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    val totalFormatted = "$currency${numberFormatter.format(totalExpenditure)}"
    val transfersFormatted = "$currency${numberFormatter.format(transfersTotal)}"
    val spendsFormatted = "$currency${numberFormatter.format(spendsTotal)}"
    val creditCardsFormatted = "$currency${numberFormatter.format(creditCardsTotal)}"
    val selfFormatted = "$currency${numberFormatter.format(selfTotal)}"
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
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = GlassCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder)
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
                        color = SavioSlateMuted
                    )
                    Text(
                        text = "Net Monthly Expenditure",
                        style = MaterialTheme.typography.bodySmall,
                        color = SavioSlateBody
                    )
                }

                // Status Bar Notification Pill Indicator
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SavioSlateDark,
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

            // Main Large Amount Display
            Text(
                text = totalFormatted,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.0).sp
                ),
                color = SavioSlateDark
            )

            // Glassmorphic Salary & Live Savings Summary Card
            if (monthlySalary > 0) {
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
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
                                    color = SavioSlateMuted
                                )
                                Text(
                                    text = salaryFormatted,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SavioSlateDark
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (monthlySavings >= 0) "Amount Saved" else "Overspent Deficit",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = SavioSlateMuted
                                )
                                Text(
                                    text = savingsFormatted,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (monthlySavings >= 0) SavioEmerald else SavioSpendRose
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
                                    color = SavioSlateMuted
                                )
                                Text(
                                    text = if (monthlySavings >= savingsGoal) "Goal Achieved! ($goalPercent%)" else "$goalPercent% achieved",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (monthlySavings >= savingsGoal) SavioEmerald else SavioTransferIndigo
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LinearProgressIndicator(
                                progress = { goalProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (monthlySavings >= savingsGoal) SavioEmerald else SavioTransferIndigo,
                                trackColor = SavioEmeraldContainer
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
                            color = SavioSlateDark
                        )
                        Text(
                            text = if (totalExpenditure > monthlyBudget) {
                                "Exceeded by $currency${numberFormatter.format(totalExpenditure - monthlyBudget)}"
                            } else {
                                "$currency${numberFormatter.format(remaining)} left ($percentInt%)"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (totalExpenditure > monthlyBudget) SavioSpendRose else SavioEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (totalExpenditure > monthlyBudget) SavioSpendRose else SavioEmerald,
                        trackColor = SavioEmeraldContainer
                    )
                }
            }

            if (safeSpendPacing != null && monthlyBudget > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                val (badgeBg, badgeTextColor, badgeLabel) = when (safeSpendPacing.status) {
                    com.example.ui.PacingStatus.ON_TRACK -> Triple(SavioEmeraldContainer, SavioEmerald, "On Track")
                    com.example.ui.PacingStatus.CAUTION -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Caution")
                    com.example.ui.PacingStatus.OVER_PACED -> Triple(SavioSpendRoseBg, SavioSpendRose, "Over Pacing")
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(badgeTextColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Safe Daily Spend Pacing",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SavioSlateDark
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = badgeLabel,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = badgeTextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$currency${numberFormatter.format(safeSpendPacing.safeDailySpend)}/day",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp
                                ),
                                color = SavioSlateDark
                            )

                            Text(
                                text = "Today: $currency${numberFormatter.format(safeSpendPacing.todaySpent)} • ${safeSpendPacing.daysRemaining}d left",
                                style = MaterialTheme.typography.labelSmall,
                                color = SavioSlateMuted
                            )
                        }

                        if (safeSpendPacing.upcomingRecurringTotal > 0 || upcomingCommitmentsCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SavioEmeraldContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = onUpcomingCommitmentsClick != null) {
                                        onUpcomingCommitmentsClick?.invoke()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.EventRepeat,
                                            contentDescription = null,
                                            tint = SavioEmerald,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$upcomingCommitmentsCount upcoming bill(s) • $currency${numberFormatter.format(safeSpendPacing.upcomingRecurringTotal)} reserved",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = SavioSlateDark
                                        )
                                    }

                                    Text(
                                        text = "View Radar ↗",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = SavioEmerald
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Breakdown Tiles: 4 Types (Merchants, P2P Transfers, Credit Cards, Self)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpendCategoryPill(
                        label = "Spends (Merchants)",
                        amount = spendsFormatted,
                        icon = Icons.Default.Storefront,
                        accentColor = SavioSpendRose,
                        backgroundColor = SavioSpendRoseBg,
                        modifier = Modifier.weight(1f)
                    )

                    SpendCategoryPill(
                        label = "Transfers (P2P)",
                        amount = transfersFormatted,
                        icon = Icons.Default.SwapHoriz,
                        accentColor = SavioTransferIndigo,
                        backgroundColor = SavioTransferIndigoBg,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpendCategoryPill(
                        label = "Credit Cards",
                        amount = creditCardsFormatted,
                        icon = Icons.Default.CreditCard,
                        accentColor = Color(0xFF7C3AED),
                        backgroundColor = Color(0xFFF5F3FF),
                        modifier = Modifier.weight(1f)
                    )

                    SpendCategoryPill(
                        label = "Self Transfers",
                        amount = selfFormatted,
                        icon = Icons.Default.AccountBalance,
                        accentColor = SavioEmerald,
                        backgroundColor = SavioEmeraldContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
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
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = SavioSlateMuted
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = amount,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = accentColor,
                maxLines = 1
            )
        }
    }
}
