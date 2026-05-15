package com.colormagic.kids.presentation.screens.subscription

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.domain.model.UserProfile
import com.colormagic.kids.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlanTier { Starter, Unlimited, Refill }

data class SubscriptionPlan(
    val id: PlanTier,
    val name: String,
    val tagline: String,
    val price: String,
    val priceSuffix: String? = null,
    val features: List<String>,
    val isCurrent: Boolean = false,
    val isBestValue: Boolean = false
)

data class SubscriptionUiState(
    val plans: List<SubscriptionPlan> = defaultPlans,
    val selectedPlanId: PlanTier = PlanTier.Unlimited,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val authRepository: AuthRepository
    // TODO: also inject BillingClient wrapper when Play Billing is wired up.
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    /** Reactive auth state for the screen — drives the inline hint above the Continue button. */
    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser

    fun onPlanSelected(id: PlanTier) =
        _uiState.update { it.copy(selectedPlanId = id) }

    /**
     * Single entry point for the Continue button. Encapsulates the
     * "auth before paying" rule:
     *   1. If the parent isn't signed in, run sign-in first.
     *   2. Only after a successful sign-in do we hand off to Play Billing.
     *   3. Any failure surfaces as an [errorMessage] in [uiState].
     *
     * Wrapping both steps here means the screen never has to coordinate
     * two ViewModels with a pending-flag dance — the VM owns the rule.
     */
    fun attemptSubscribe(
        activity: FragmentActivity,
        onCompleted: () -> Unit
    ) {
        if (_uiState.value.isProcessing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            if (!authRepository.isSignedIn) {
                val signInResult = authRepository.signInWithGoogle(activity)
                if (signInResult.isFailure) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "Sign-in is required to subscribe."
                        )
                    }
                    return@launch
                }
            }

            // TODO: hand off to BillingClient.launchBillingFlow(...).
            // For the stub flow we proceed straight to the success callback.

            _uiState.update { it.copy(isProcessing = false) }
            onCompleted()
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

private val defaultPlans = listOf(
    SubscriptionPlan(
        id = PlanTier.Starter,
        name = "Starter Pack",
        tagline = "Try out the magic.",
        price = "Free",
        features = listOf("3 sketches per day", "Basic tools"),
        isCurrent = true
    ),
    SubscriptionPlan(
        id = PlanTier.Unlimited,
        name = "Unlimited Magic",
        tagline = "Never run out of paper.",
        price = "$4.99",
        priceSuffix = "/mo",
        features = listOf("50 sketches per month", "All magical tools", "Save to Gallery"),
        isBestValue = true
    ),
    SubscriptionPlan(
        id = PlanTier.Refill,
        name = "Magic Refill",
        tagline = "A quick boost of creativity.",
        price = "$1.99",
        features = listOf("20 extra sketches", "Never expires")
    )
)
