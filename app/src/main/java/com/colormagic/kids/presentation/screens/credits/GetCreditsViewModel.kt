package com.colormagic.kids.presentation.screens.credits

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.data.telemetry.AppTelemetry
import com.colormagic.kids.domain.model.UserQuota
import com.colormagic.kids.domain.repository.CreditRepository
import com.colormagic.kids.domain.repository.RewardedAdGrantResult
import com.colormagic.kids.monetization.CreditConfig
import com.colormagic.kids.monetization.AdLoadState
import com.colormagic.kids.monetization.RewardedAdManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GetCreditsUiState(
    val quota: UserQuota = UserQuota.UNKNOWN,
    val adLoadState: AdLoadState = AdLoadState.Idle,
    val isGranting: Boolean = false,        // waiting for backend grant after ad
    val toastMessage: String? = null,       // one-shot feedback message
    val isLoading: Boolean = false
) {
    val canWatchAd: Boolean
        get() = adLoadState == AdLoadState.Ready &&
            !isGranting &&
            quota.rewardedAdsRemaining > 0 &&
            !quota.isPremium

    val watchAdButtonLabel: String
        get() = when {
            quota.isPremium -> "No ads needed — you're premium!"
            quota.rewardedAdsRemaining <= 0 -> "Ad limit reached for today"
            adLoadState == AdLoadState.Loading -> "Preparing your reward…"
            adLoadState == AdLoadState.Showing || isGranting -> "Earning credits…"
            adLoadState == AdLoadState.Failed -> "Ad not available right now"
            else -> "Watch Ad  +${CreditConfig.REWARDED_AD_CREDITS} Credits"
        }
}

@HiltViewModel
class GetCreditsViewModel @Inject constructor(
    private val creditRepository: CreditRepository,
    private val rewardedAdManager: RewardedAdManager,
    private val telemetry: AppTelemetry
) : ViewModel() {

    private val _uiState = MutableStateFlow(GetCreditsUiState())
    val uiState: StateFlow<GetCreditsUiState> = _uiState.asStateFlow()

    init {
        // Mirror ad load state into UI state
        viewModelScope.launch {
            rewardedAdManager.adLoadState.collect { loadState ->
                _uiState.update { it.copy(adLoadState = loadState) }
            }
        }
        // Mirror live quota into UI state
        viewModelScope.launch {
            creditRepository.quotaFlow.collect { quota ->
                _uiState.update { it.copy(quota = quota) }
            }
        }
    }

    fun onScreenOpened() {
        telemetry.logCreditEvent("credits_screen_opened")
        refreshAndPreloadAd()
    }

    private fun refreshAndPreloadAd() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            creditRepository.refreshQuota()
            _uiState.update { it.copy(isLoading = false) }
        }
        // Pre-load ad so it is ready when the user taps the button
        if (!_uiState.value.quota.isPremium) {
            rewardedAdManager.preload()
        }
    }

    /**
     * User tapped "Watch Ad". Shows the rewarded ad; on completion calls the
     * backend to grant credits.
     */
    fun onWatchAdTapped(activity: Activity) {
        if (!_uiState.value.canWatchAd) return
        telemetry.logCreditEvent("rewarded_ad_requested")

        rewardedAdManager.showAd(
            activity = activity,
            onAdEarned = {
                // Ad completed — now tell the backend
                viewModelScope.launch { handleAdEarned() }
            },
            onAdFailed = { reason ->
                _uiState.update {
                    it.copy(toastMessage = "Ad isn't available right now. Please try again in a moment.")
                }
                telemetry.logCreditEvent("rewarded_ad_failed")
            }
        )
    }

    private suspend fun handleAdEarned() {
        _uiState.update { it.copy(isGranting = true) }
        when (val result = creditRepository.grantRewardedAdCredits()) {
            is RewardedAdGrantResult.Success -> {
                _uiState.update {
                    it.copy(
                        isGranting = false,
                        toastMessage = "${result.creditsGranted} credits added."
                    )
                }
            }
            RewardedAdGrantResult.DailyLimitReached -> {
                _uiState.update {
                    it.copy(
                        isGranting = false,
                        toastMessage = "You've reached today's rewarded ad limit. " +
                            "Come back tomorrow or upgrade for more credits."
                    )
                }
            }
            is RewardedAdGrantResult.Failed -> {
                _uiState.update {
                    it.copy(
                        isGranting = false,
                        toastMessage = "Couldn't add credits right now. Please try again."
                    )
                }
            }
        }
    }

    fun onToastShown() = _uiState.update { it.copy(toastMessage = null) }
}
