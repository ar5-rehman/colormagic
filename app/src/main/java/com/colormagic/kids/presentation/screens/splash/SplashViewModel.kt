package com.colormagic.kids.presentation.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.data.local.preferences.OnboardingPreferences
import com.colormagic.kids.domain.repository.AuthRepository
import com.colormagic.kids.presentation.navigation.RootDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// Kicks off anonymous sign-in, then decides where to go after the splash:
//   • first launch / fresh install → Onboarding (then the welcome paywall)
//   • returning user               → straight to Main
//
// The onboarding-completed flag is persisted, so the intro + paywall only
// ever show once per install.
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    private val _nextRoute = MutableStateFlow<String?>(null)
    /** Where to navigate once the splash finishes. Null until decided. */
    val nextRoute: StateFlow<String?> = _nextRoute.asStateFlow()

    init {
        // Fire-and-forget anonymous sign-in (resolves well within the splash).
        viewModelScope.launch { authRepository.ensureSignedIn() }

        viewModelScope.launch {
            val completed = onboardingPreferences.completed.first() // local, instant
            delay(SPLASH_MIN_MS) // let the branding show for a moment
            _nextRoute.value =
                if (completed) RootDestination.MAIN else RootDestination.ONBOARDING
        }
    }

    private companion object {
        const val SPLASH_MIN_MS = 1800L
    }
}
