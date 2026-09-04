package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.SpendTrackerApplication
import com.example.service.LiveExpenditureNotificationService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val app = context.applicationContext as? SpendTrackerApplication
            val isEnabled = app?.preferences?.isPersistentNotificationEnabled ?: true
            if (isEnabled) {
                LiveExpenditureNotificationService.updateLiveExpenditure(context)
            }
        }
    }
}
