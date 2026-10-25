package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.service.LiveExpenditureNotificationService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationReadinessTest {
    @Test fun rendersAnOngoingNotificationWithActionsWithoutStartingAService() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<SpendTrackerApplication>()
        app.preferences.isPersistentNotificationEnabled = true
        LiveExpenditureNotificationService.refreshNotification(app)
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = manager.activeNotifications.single { it.id == LiveExpenditureNotificationService.NOTIFICATION_ID }.notification
        assertTrue(notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(2, notification.actions.size)
        assertNotNull(notification.contentIntent)
        app.preferences.isPersistentNotificationEnabled = false
        LiveExpenditureNotificationService.refreshNotification(app)
        assertFalse(manager.activeNotifications.any { it.id == LiveExpenditureNotificationService.NOTIFICATION_ID })
    }

    @Test fun compatibilityServiceIsNotStickyAndStopsWhenAsked() {
        val controller = Robolectric.buildService(LiveExpenditureNotificationService::class.java).create()
        val service = controller.get()
        assertEquals(android.app.Service.START_NOT_STICKY,
            service.onStartCommand(Intent(service, LiveExpenditureNotificationService::class.java).setAction(LiveExpenditureNotificationService.ACTION_STOP), 0, 1))
        controller.destroy()
    }
}
