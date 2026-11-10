package com.colormagic.kids.presentation

import com.colormagic.kids.data.telemetry.AnalyticsEvent
import com.colormagic.kids.data.telemetry.AppTelemetry
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
    private val onboardingPreferences: OnboardingPreferences,
    private val telemetry: AppTelemetry
) : ViewModel() {

    fun markOnboardingComplete() {
        telemetry.logEvent(AnalyticsEvent.ONBOARDING_COMPLETED)
        viewModelScope.launch { onboardingPreferences.setCompleted() }
    }
}
