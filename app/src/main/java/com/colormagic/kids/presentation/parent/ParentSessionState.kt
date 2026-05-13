package com.colormagic.kids.presentation.parent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Session-scoped "has the parent already authenticated this run of the app?"
// flag. Reset on process death (constructor default = false), so each cold
// start re-prompts. Kept singleton so the value survives bottom-nav tab
// switches and screen rotations.
@Singleton
class ParentSessionState @Inject constructor() {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun markAuthenticated() {
        _isAuthenticated.value = true
    }

    fun reset() {
        _isAuthenticated.value = false
    }
}
