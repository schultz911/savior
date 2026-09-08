package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SavioEmerald
import com.example.ui.theme.StatusActiveGreen
import kotlinx.coroutines.launch

@Composable
fun ScanInboxButton(
    isSyncing: Boolean,
    onSyncInbox: () -> Unit,
    onResetAndRescan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val progress = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .testTag("sync_inbox_button")
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = if (isHolding) SavioEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(isSyncing) {
                if (isSyncing) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isHolding = true
                    var completed = false
                    val animJob = scope.launch {
                        progress.snapTo(0f)
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
                        )
                        completed = true
                    }
                    val up = waitForUpOrCancellation()
                    animJob.cancel()
                    isHolding = false

                    if (completed || progress.value >= 0.98f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { progress.snapTo(0f) }
                        onResetAndRescan()
                    } else {
                        val wasQuickTap = progress.value < 0.25f
                        scope.launch { progress.animateTo(0f, tween(150)) }
                        if (up != null && wasQuickTap) {
                            onSyncInbox()
                        }
                    }
                }
            }
    ) {
        // Visual animated progress bar filling horizontally across the button
        if (progress.value > 0f) {
            Box(
                modifier = Modifier.matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.value)
                        .background(SavioEmerald.copy(alpha = 0.35f))
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Sync",
                tint = if (isHolding) SavioEmerald else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when {
                    isSyncing -> "Scanning..."
                    isHolding -> "Hold to Rescan..."
                    else -> "Scan Inbox"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isHolding) SavioEmerald else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PermissionsBanner(
    hasSmsPermissions: Boolean,
    hasNotificationPermission: Boolean,
    isSyncing: Boolean,
    onRequestPermissions: () -> Unit,
    onSyncInbox: () -> Unit,
    onResetAndRescan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("permissions_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasSmsPermissions && hasNotificationPermission) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasSmsPermissions && hasNotificationPermission) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Shield
                        },
                        contentDescription = null,
                        tint = if (hasSmsPermissions && hasNotificationPermission) {
                            StatusActiveGreen
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasSmsPermissions && hasNotificationPermission) {
                            "SMS Listener Active"
                        } else {
                            "Enable Live SMS Sync"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (hasSmsPermissions) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ScanInboxButton(
                        isSyncing = isSyncing,
                        onSyncInbox = onSyncInbox,
                        onResetAndRescan = onResetAndRescan
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (hasSmsPermissions && hasNotificationPermission) {
                    "Listening for bank SMS in real-time. Debits, transfers, and spends are calculated locally with zero cloud upload. Tap 'Scan Inbox' to sync, or press & hold to reset and rescan from scratch."
                } else {
                    "Grant SMS and Notification permissions so the app can automatically detect bank alerts and show your live monthly spend in the status bar."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasSmsPermissions || !hasNotificationPermission) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestPermissions,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_permissions_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant SMS & Notification Permissions")
                }
            }
        }
    }
}
