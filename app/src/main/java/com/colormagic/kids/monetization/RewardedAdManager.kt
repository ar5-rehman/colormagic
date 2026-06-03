package com.colormagic.kids.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.colormagic.kids.data.telemetry.AppTelemetry
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AdLoadState { Idle, Loading, Ready, Showing, Failed }

/**
 * Manages the lifecycle of a single rewarded video ad.
 *
 * Usage:
 *   1. Call [preload] after initialisation (or after showing/failing an ad).
 *   2. Observe [adLoadState] to enable/disable the "Watch Ad" button.
 *   3. Call [showAd] when the user taps. The [onAdEarned] callback fires only
 *      when the ad completes and the reward is confirmed by the SDK.
 *
 * COPPA / Play Families policy:
 *   This manager must only be used inside the parent-gated area of the app.
 *   Rewarded ads are not permitted in the child-directed content flow per
 *   Google Play Families policy. [MobileAds] is configured with
 *   TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE here so the ad request itself is
 *   child-safe if it ever escapes the parent gate, but the UX entry points
 *   must remain behind the parent gate in app code.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telemetry: AppTelemetry
) {

    private val _adLoadState = MutableStateFlow(AdLoadState.Idle)
    val adLoadState: StateFlow<AdLoadState> = _adLoadState.asStateFlow()

    private var rewardedAd: RewardedAd? = null

    init {
        // Child-directed treatment required for this app under COPPA.
        val config = RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
            .build()
        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(context)
    }

    /** Pre-loads a rewarded ad. Call this as early as possible so the ad is
     *  ready by the time the user taps "Watch Ad". */
    fun preload() {
        if (_adLoadState.value == AdLoadState.Loading ||
            _adLoadState.value == AdLoadState.Ready ||
            _adLoadState.value == AdLoadState.Showing
        ) return

        _adLoadState.value = AdLoadState.Loading
        telemetry.logCreditEvent("rewarded_ad_loaded") // "requested" → will emit "loaded" or "failed"

        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            CreditConfig.REWARDED_AD_UNIT_ID,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _adLoadState.value = AdLoadState.Ready
                    Log.d(TAG, "Rewarded ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _adLoadState.value = AdLoadState.Failed
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                    telemetry.logCreditEvent("rewarded_ad_failed")
                }
            }
        )
    }

    /**
     * Shows the loaded rewarded ad.
     *
     * @param activity       The foreground activity (required by the SDK).
     * @param onAdEarned     Called when the SDK confirms the user earned the
     *                       reward. Call [CreditRepository.grantRewardedAdCredits]
     *                       from here to actually credit the account.
     * @param onAdFailed     Called when the ad cannot be shown (not loaded,
     *                       dismissed before earning, or show error).
     * @param onAdDismissed  Called after the ad closes regardless of outcome.
     */
    fun showAd(
        activity: Activity,
        onAdEarned: () -> Unit,
        onAdFailed: (String) -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null || _adLoadState.value != AdLoadState.Ready) {
            onAdFailed("Ad isn't available right now. Please try again in a moment.")
            return
        }

        _adLoadState.value = AdLoadState.Showing
        var rewardEarned = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _adLoadState.value = AdLoadState.Idle
                onAdDismissed()
                if (!rewardEarned) {
                    // User closed the ad before earning the reward
                    onAdFailed("Ad was closed early. Complete the ad to earn credits.")
                }
                // Pre-load the next ad
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                _adLoadState.value = AdLoadState.Failed
                Log.w(TAG, "Rewarded ad failed to show: ${error.message}")
                telemetry.logCreditEvent("rewarded_ad_failed")
                onAdFailed("Ad isn't available right now. Please try again in a moment.")
                preload()
            }
        }

        ad.show(activity) {
            // The SDK fires this only when the reward condition is met
            rewardEarned = true
            telemetry.logCreditEvent("rewarded_ad_completed")
            onAdEarned()
        }
    }

    private companion object {
        const val TAG = "RewardedAdManager"
    }
}
