package com.example.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppSecurityManager {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L

    @Volatile
    var isAwaitingActivityResult: Boolean = false

    @Volatile
    private var lastActivityResultCompletionTimestamp: Long = 0L

    @Volatile
    private var isPromptActive: Boolean = false

    fun markAwaitingActivityResult() {
        isAwaitingActivityResult = true
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun onActivityResultCompleted() {
        isAwaitingActivityResult = false
        lastActivityResultCompletionTimestamp = System.currentTimeMillis()
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun canAuthenticate(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                @Suppress("DEPRECATION")
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            }
            val res = biometricManager.canAuthenticate(authenticators)
            if (res == BiometricManager.BIOMETRIC_SUCCESS) {
                true
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                keyguardManager?.isDeviceSecure == true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun onAppForegrounded(isBiometricEnabled: Boolean, lockTimeoutSeconds: Int) {
        if (!isBiometricEnabled) {
            _isLocked.value = false
            return
        }

        val now = System.currentTimeMillis()
        if (isAwaitingActivityResult || (now - lastActivityResultCompletionTimestamp < 3000L)) {
            // Returning from permission dialog, SAF file picker, or activity result initiated inside app
            isAwaitingActivityResult = false
            lastBackgroundTimestamp = now
            return
        }

        if (lastBackgroundTimestamp == 0L) {
            // First cold launch of app session
            _isLocked.value = true
            return
        }

        val elapsedSeconds = (now - lastBackgroundTimestamp) / 1000
        if (elapsedSeconds >= lockTimeoutSeconds) {
            _isLocked.value = true
        }
    }

    fun onAppBackgrounded() {
        if (!isAwaitingActivityResult) {
            lastBackgroundTimestamp = System.currentTimeMillis()
        }
    }

    fun lock() {
        _isLocked.value = true
    }

    fun unlock() {
        _isLocked.value = false
        isPromptActive = false
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "Savio₹ Secured",
        subtitle: String = "Confirm biometric or device PIN to access your financial data",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isPromptActive) return
        if (activity.isFinishing || activity.isDestroyed) return
        if (activity.supportFragmentManager.isStateSaved) return

        if (!canAuthenticate(activity)) {
            unlock()
            onSuccess()
            return
        }

        try {
            isPromptActive = true
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isPromptActive = false
                        unlock()
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        isPromptActive = false
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                        ) {
                            onError("Tap 'Unlock Savio₹' to authenticate.")
                        } else {
                            onError(errString.toString())
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Authentication failed. Please try again.")
                    }
                }
            )

            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                promptInfoBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                @Suppress("DEPRECATION")
                promptInfoBuilder.setDeviceCredentialAllowed(true)
            }

            prompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            isPromptActive = false
            onError(e.localizedMessage ?: "Biometric prompt error")
        }
    }
}
