package com.colormagic.kids.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.data.local.preferences.OnboardingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** App-entry state: lets AppRoot mark the first-run flow (onboarding + welcome
 *  paywall) as seen so it never shows again on this install. */
@HiltViewModel
class AppEntryViewModel @Inject constructor(
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    fun markOnboardingComplete() {
        viewModelScope.launch { onboardingPreferences.setCompleted() }
    }
}
