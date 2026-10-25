package com.example.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

/** Refreshes the ongoing status notification without keeping a foreground service alive. */
class NotificationRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        LiveExpenditureNotificationService.refreshNotification(applicationContext)
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        android.util.Log.e("NotificationRefresh", "Could not refresh spend status", e)
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
