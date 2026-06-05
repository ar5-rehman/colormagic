package com.colormagic.kids.presentation.screens.parents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.colormagic.kids.presentation.parent.ParentSessionState
import com.colormagic.kids.presentation.screens.parentgate.ParentGateScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Bottom-nav Parents entry. The gate is ALWAYS required when entering this
// tab — there is no opt-out, no toggle, no first-time bypass. The kid has to
// pass biometric / device-credential authentication via [ParentGateScreen]
// before the [ParentAreaScreen] is rendered.
//
// Auth state lives in [ParentSessionState], a process-scoped singleton — it
// survives bottom-nav tab switches and screen rotations but resets on cold
// start, so each new app launch re-prompts.
@Composable
fun ParentsScreen(
    onManageSubscription: () -> Unit,
    onOpenSupport: () -> Unit,
    onLeaveTab: () -> Unit,
    router: ParentsRouterViewModel = hiltViewModel()
) {
    val isAuthenticated by router.sessionState.isAuthenticated.collectAsState()

    if (!isAuthenticated) {
        ParentGateScreen(
            onUnlocked = { /* sessionState now true → recomposes into the area */ },
            onCancel = onLeaveTab
        )
    } else {
        ParentAreaScreen(
            onManageSubscription = onManageSubscription,
            onOpenSupport = onOpenSupport
        )
    }
}

@HiltViewModel
class ParentsRouterViewModel @Inject constructor(
    val sessionState: ParentSessionState
) : ViewModel()
