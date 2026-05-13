package com.colormagic.kids.presentation.parent

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

// Thin wrapper around BiometricPrompt so the screen layer never has to deal
// with executors, callbacks, or builder boilerplate.
//
// Auth strategy: prefer BIOMETRIC_STRONG / BIOMETRIC_WEAK, but allow
// DEVICE_CREDENTIAL fallback so devices without fingerprint hardware can
// still gate via PIN / pattern.
@Singleton
class BiometricAuthenticator @Inject constructor() {

    enum class Capability { Available, NoneEnrolled, Unsupported }

    fun capability(context: Context): Capability {
        val manager = BiometricManager.from(context)
        val allowed = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        return when (manager.canAuthenticate(allowed)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Capability.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Capability.NoneEnrolled
            else -> Capability.Unsupported
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Grown-ups only",
        subtitle: String = "Authenticate to enter the Parent Area",
        onSuccess: () -> Unit,
        onCancelled: () -> Unit = {},
        onError: (message: String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onCancelled()
                        else -> onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    // A specific attempt failed — the prompt stays open. No-op.
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }
}
