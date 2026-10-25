package com.example

import android.hardware.biometrics.BiometricManager
import androidx.fragment.app.FragmentActivity
import com.example.security.AppSecurityManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBiometricManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityReadinessTest {
    @Test fun authenticationAvailabilityErrorsNeverUnlockOrReportSuccess() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        val activity = controller.get()
        val shadow = Shadow.extract<ShadowBiometricManager>(activity.getSystemService(BiometricManager::class.java))
        shadow.setCanAuthenticate(false)
        for (error in listOf(1, 11, 12, 15)) {
            shadow.setAuthenticatorType(error)
            AppSecurityManager.lock()
            var successes = 0
            var message: String? = null
            AppSecurityManager.promptBiometric(activity, onSuccess = { successes++ }, onError = { message = it })
            assertEquals(0, successes)
            assertTrue(AppSecurityManager.isLocked.value)
            assertNotNull(message)
        }
        controller.pause().stop().destroy()
    }

    @Test fun returningFromFilePickerCannotClearAnExistingLock() {
        AppSecurityManager.lock()
        AppSecurityManager.markAwaitingActivityResult()
        AppSecurityManager.onActivityResultCompleted()
        AppSecurityManager.onAppForegrounded(true, 0)
        assertTrue(AppSecurityManager.isLocked.value)
    }
}
