package com.colormagic.kids.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.domain.model.AuthUser
import com.colormagic.kids.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Surfaces the anonymous Firebase identity to the UI and exposes sign-out.
// There's no "sign in" action — that happens automatically at launch
// (see SplashViewModel), so this VM is intentionally tiny.
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthUser?> = authRepository.authState

    init {
        // Retry anonymous sign-in if it hasn't resolved yet (e.g. Splash's
        // attempt failed transiently, or the parent opened the app deep-
        // linked into the Parent Area without ever showing Splash).
        // ensureSignedIn is a no-op when a user already exists.
        viewModelScope.launch { authRepository.ensureSignedIn() }
    }

    /**
     * Signs out the anonymous account. The next launch mints a fresh uid,
     * so any credits/sketches on the old account become unreachable —
     * callers should confirm with the parent before invoking this.
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
