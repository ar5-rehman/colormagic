package com.colormagic.kids.presentation.screens.parentgate

import androidx.lifecycle.ViewModel
import com.colormagic.kids.data.local.preferences.ParentGatePreferences
import com.colormagic.kids.presentation.parent.BiometricAuthenticator
import com.colormagic.kids.presentation.parent.ParentSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ParentGateViewModel @Inject constructor(
    val biometricAuthenticator: BiometricAuthenticator,
    private val sessionState: ParentSessionState,
    private val gatePrefs: ParentGatePreferences
) : ViewModel() {

    val isGateEnabled: Boolean get() = gatePrefs.isGateEnabled

    fun grantAccess() {
        sessionState.markAuthenticated()
    }
}
