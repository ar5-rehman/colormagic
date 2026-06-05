package com.colormagic.kids.presentation.screens.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.data.telemetry.AppTelemetry
import com.colormagic.kids.domain.model.BillingProducts
import com.colormagic.kids.domain.model.PurchaseResult
import com.colormagic.kids.domain.repository.BillingRepository
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
    val isRestoring: Boolean = false,
    val errorMessage: String? = null,
    val restoreMessage: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val telemetry: AppTelemetry
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        // Connect to Play + preload product details so Continue is instant.
        viewModelScope.launch { billingRepository.start() }
        telemetry.logCreditEvent("premium_screen_opened")
    }

    fun onPlanSelected(id: PlanTier) =
        _uiState.update { it.copy(selectedPlanId = id) }

    /**
     * Continue button entry point. The parent already has an anonymous
     * Firebase identity (created at launch), so there's no sign-in step —
     * this launches the Google Play purchase flow. The completed purchase is
     * verified server-side ([BillingRepository.purchase]) before the user is
     * granted anything.
     */
    fun onContinue(activity: Activity, onCompleted: () -> Unit) {
        if (_uiState.value.isProcessing) return

        val productId = when (_uiState.value.selectedPlanId) {
            PlanTier.Unlimited -> BillingProducts.MONTHLY_PRO
            PlanTier.Refill -> BillingProducts.EXTRA_20
            // Starter is the free plan — nothing to purchase.
            PlanTier.Starter -> {
                onCompleted()
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            when (val result = billingRepository.purchase(activity, productId)) {
                is PurchaseResult.Success -> {
                    telemetry.logCreditEvent("subscription_started")
                    _uiState.update { it.copy(isProcessing = false) }
                    onCompleted()
                }
                PurchaseResult.Cancelled ->
                    _uiState.update { it.copy(isProcessing = false) }
                PurchaseResult.Pending ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "Your purchase is pending. We'll unlock the magic once it clears."
                        )
                    }
                is PurchaseResult.Failed ->
                    _uiState.update {
                        it.copy(isProcessing = false, errorMessage = result.message)
                    }
            }
        }
    }

    fun onRestorePurchases() {
        if (_uiState.value.isRestoring) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, restoreMessage = null) }
            when (val result = billingRepository.restorePurchases()) {
                is PurchaseResult.Success -> {
                    telemetry.logCreditEvent("subscription_restored")
                    _uiState.update {
                        it.copy(isRestoring = false, restoreMessage = "Purchase restored successfully.")
                    }
                }
                is PurchaseResult.Failed ->
                    _uiState.update {
                        it.copy(isRestoring = false, restoreMessage = result.message)
                    }
                else ->
                    _uiState.update {
                        it.copy(isRestoring = false, restoreMessage = "Nothing to restore.")
                    }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissRestoreMessage() {
        _uiState.update { it.copy(restoreMessage = null) }
    }
}

private val defaultPlans = listOf(
    SubscriptionPlan(
        id = PlanTier.Starter,
        name = "Free",
        tagline = "Try out the magic.",
        price = "Free",
        features = listOf(
            "Watch ads to earn +3 credits",
            "Up to 5 rewarded ads/day",
            "Save to Gallery — always free",
            "Basic coloring tools"
        ),
        isCurrent = true
    ),
    SubscriptionPlan(
        id = PlanTier.Unlimited,
        name = "Premium",
        tagline = "Create more with fewer limits.",
        price = "$4.99",
        priceSuffix = "/mo",
        features = listOf(
            "30 credits every day",
            "No ads — ever",
            "All premium coloring tools",
            "Priority generation"
        ),
        isBestValue = true
    ),
    SubscriptionPlan(
        id = PlanTier.Refill,
        name = "Credit Refill",
        tagline = "A quick boost of creativity.",
        price = "$1.99",
        features = listOf("20 extra credits", "Never expires", "Use anytime")
    )
)
