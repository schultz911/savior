package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.SpendTrackerApplication
import com.example.sms.SmsReader
import java.util.concurrent.TimeUnit

/**
 * Background SMS Reliability Watchdog.
 * Periodically recovers and ingests banking & debit SMS messages that might have been
 * dropped or delayed by aggressive OEM battery optimization / Doze mode.
 */
class SmsCatchUpWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "SmsCatchUpWorker running background sync check...")
        try {
            if (!SmsReader.hasReadSmsPermission(applicationContext)) {
                Log.d(TAG, "SmsCatchUpWorker: SMS permissions not granted, skipping.")
                return Result.success()
            }

            val app = applicationContext as? SpendTrackerApplication ?: return Result.success()
            val insertedCount = app.repository.syncInbox()
            Log.d(TAG, "SmsCatchUpWorker completed. Ingested $insertedCount missed messages.")

            if (insertedCount > 0 && app.preferences.isPersistentNotificationEnabled) {
                LiveExpenditureNotificationService.updateLiveExpenditure(applicationContext)
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SmsCatchUpWorker failed with error: ${e.message}", e)
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "SmsCatchUpWorker"
        const val WORK_NAME = "savior_sms_catchup_periodic_worker"

        fun schedule(context: Context) {
            try {
                val workRequest = PeriodicWorkRequestBuilder<SmsCatchUpWorker>(
                    6, TimeUnit.HOURS,
                    30, TimeUnit.MINUTES // 30 minute flex interval
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
                Log.d(TAG, "Periodic SmsCatchUpWorker scheduled successfully (every 6 hours).")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule SmsCatchUpWorker: ${e.message}", e)
            }
        }
    }
}
