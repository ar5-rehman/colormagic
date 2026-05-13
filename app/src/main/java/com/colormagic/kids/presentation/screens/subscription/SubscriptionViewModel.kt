package com.colormagic.kids.presentation.screens.subscription

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val selectedPlanId: PlanTier = PlanTier.Unlimited
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    // TODO: inject BillingClient wrapper when Play Billing is wired up.
) : ViewModel() {
    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    fun onPlanSelected(id: PlanTier) =
        _uiState.update { it.copy(selectedPlanId = id) }

    fun onContinue() {
        // TODO: hand off to Play Billing flow.
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
