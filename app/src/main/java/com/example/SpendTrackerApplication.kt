package com.example

import android.app.Application
import android.content.Context
import com.example.data.AppDatabase
import com.example.data.ExpensePreferences
import com.example.data.ExpenseRepository
import com.example.service.LiveExpenditureNotificationService

class SpendTrackerApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ExpenseRepository
        private set

    lateinit var preferences: ExpensePreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        preferences = ExpensePreferences(this)
        repository = ExpenseRepository(this, database.expenseDao(), preferences, database.merchantRuleDao())

        // Initialize Notification Channels
        LiveExpenditureNotificationService.createNotificationChannel(this)

        // Schedule Background SMS Reliability Watchdog (Doze Protection)
        com.example.service.SmsCatchUpWorker.schedule(this)

        // If persistent notification is enabled, ensure it's started/synced
        if (preferences.isPersistentNotificationEnabled) {
            try {
                LiveExpenditureNotificationService.updateLiveExpenditure(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        lateinit var instance: SpendTrackerApplication
            private set

        fun getAppContext(): Context = instance.applicationContext
    }
}
