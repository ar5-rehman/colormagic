package com.colormagic.kids.presentation.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Kicks off anonymous Firebase sign-in the moment the splash appears.
//
// Fire-and-forget: anonymous auth resolves in well under the splash's
// ~2.2s display window, and downstream screens observe AuthRepository
// .authState reactively — so they pick up the uid whenever it lands without
// the splash having to block on it.
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            authRepository.ensureSignedIn()
        }
    }
}
