package com.colormagic.kids.presentation.parent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.colormagic.kids.presentation.screens.parentgate.ParentGateScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Shows [content] only after a grown-up passes the parent gate (hold-to-unlock,
 * then biometric / device credential when available). Families policy: anything
 * that spends money must sit behind a parent gate.
 *
 * Shares [ParentSessionState] with the Parents tab, so a parent who already
 * unlocked this session isn't asked again until the next cold start.
 */
@Composable
fun ParentGated(
    onCancel: () -> Unit,
    content: @Composable () -> Unit
) {
    val gate: ParentGatedViewModel = hiltViewModel()
    val unlocked by gate.sessionState.isAuthenticated.collectAsState()
    if (unlocked) {
        content()
    } else {
        ParentGateScreen(
            onUnlocked = { /* session flips to authenticated → recomposes into content */ },
            onCancel = onCancel
        )
    }
}

@HiltViewModel
class ParentGatedViewModel @Inject constructor(
    val sessionState: ParentSessionState
) : ViewModel()
