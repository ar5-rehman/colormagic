package com.colormagic.kids.presentation.auth

import android.app.Activity
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.data.auth.GoogleSignInHelper
import com.colormagic.kids.data.util.NetworkMonitor
import com.colormagic.kids.domain.model.AuthUser
import com.colormagic.kids.domain.repository.AuthRepository
import com.colormagic.kids.domain.repository.CreditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Surfaces the Firebase identity to the UI: anonymous (guest) by default, with
// an optional "Sign in with Google" upgrade that links to the same account.
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInHelper: GoogleSignInHelper,
    private val creditRepository: CreditRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    val authState: StateFlow<AuthUser?> = authRepository.authState

    private val _isWorking = MutableStateFlow(false)
    /** True while a Google sign-in is in progress (disable the button). */
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    /** True during a Google sign-in flow. Suppresses the auto-recovery
     *  collectors from minting a throwaway guest between the old guest's
     *  deletion and the Google credential sign-in completing. Without this
     *  guard, the brief null-auth window causes the network/auth collectors
     *  to race and create an orphan anonymous account. */
    @Volatile
    private var signingInWithGoogle = false

    private val _message = MutableStateFlow<String?>(null)
    /** One-shot feedback to show as a toast/snackbar, then cleared. */
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    /** Non-null when we have NO identity AND sign-in failed — the UI shows the
     *  reason + a Retry button instead of an endless spinner. */
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        ensureGuest()

        // Safety net: if we ever end up with NO identity (e.g. a sign-out's
        // re-sign-in failed, or a transient launch error), recover by
        // re-establishing a guest. The 1.5s wait lets the normal re-sign-in
        // win first; this only kicks in if it didn't.
        //
        // IMPORTANT: suppressed while signingInWithGoogle — the brief null-auth
        // window between deleting the old guest and completing the Google
        // sign-in must NOT trigger a new guest creation (race → orphan docs).
        viewModelScope.launch {
            authState.collect { current ->
                if (current == null && !signingInWithGoogle) {
                    delay(1500)
                    if (authRepository.currentUser == null && !signingInWithGoogle) {
                        ensureGuest()
                    }
                } else if (current != null) {
                    _authError.value = null
                }
            }
        }

        // Auto-recover on reconnect: if the very first sign-in failed because
        // the device was offline (common on a fresh install with no network),
        // retry the moment a connection appears — no Retry tap needed.
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online && authRepository.currentUser == null && !signingInWithGoogle) {
                    ensureGuest()
                }
            }
        }
    }

    /** Ensures a guest identity exists, capturing any failure reason so it can
     *  be shown in the UI (instead of hanging on a spinner forever). */
    private fun ensureGuest() {
        viewModelScope.launch {
            // Clear the error first so the card shows the spinner again while we
            // retry — immediate visible feedback when "Try again" is tapped.
            _authError.value = null
            authRepository.ensureSignedIn()
                .onSuccess {
                    _authError.value = null
                    // Create/touch the Firestore user doc immediately so a guest
                    // exists server-side right after sign-in (the doc is created
                    // by userQuota → ensureUserDoc on the backend).
                    creditRepository.refreshQuota()
                }
                .onFailure {
                    _authError.value = it.message ?: "Couldn't connect. Check your internet."
                }
        }
    }

    /** Called by the Retry button when sign-in failed. */
    fun retrySignIn() = ensureGuest()

    /**
     * Launches the Google account picker, then links/signs in with Firebase.
     * Needs the host [activity] because Credential Manager presents UI.
     */
    fun signInWithGoogle(activity: Activity) {
        if (_isWorking.value) return
        viewModelScope.launch {
            _isWorking.value = true
            signingInWithGoogle = true
            try {
                googleSignInHelper.getGoogleIdToken(activity)
                    .onSuccess { idToken ->
                        authRepository.signInWithGoogle(idToken)
                            .onSuccess {
                                _message.value = "Signed in with Google."
                                // Same uid after linking → same Firestore doc; this
                                // just refreshes the cached balance for the (now
                                // Google) account so the UI is instantly correct.
                                creditRepository.refreshQuota()
                            }
                            .onFailure {
                                _message.value = "Couldn't sign in with Google. Please try again."
                            }
                    }
                    .onFailure { t ->
                        // Only show "cancelled" for an actual user cancel — other
                        // failures (no account, transient errors, misconfig) get a
                        // message that tells the parent what to do, instead of
                        // wrongly blaming them for cancelling.
                        _message.value = googleSignInMessage(t)
                    }
            } finally {
                signingInWithGoogle = false
                _isWorking.value = false
            }
        }
    }

    /** Maps a Credential Manager failure to a parent-friendly message. */
    private fun googleSignInMessage(t: Throwable): String = when (t) {
        is GetCredentialCancellationException -> "Sign-in cancelled."
        is NoCredentialException ->
            "No Google account found on this device. Add one in Settings, then try again."
        else -> "Couldn't sign in with Google. Please try again."
    }

    /**
     * Signs out. For a guest this starts a fresh guest account; for a
     * Google-linked account it returns to a new guest. Either way the app
     * stays usable (a fresh anonymous identity is created immediately).
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
