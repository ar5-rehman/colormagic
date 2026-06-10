package com.colormagic.kids.domain.model

import com.colormagic.kids.monetization.CreditConfig

// Mirror of the backend `userQuota` response. `totalAvailableCredits` is what
// the UI shows as the credit balance. `remainingFreeSketches` now represents
// today's daily credits (free or premium) rather than lifetime free sketches.
data class UserQuota(
    val plan: String,
    val subscriptionActive: Boolean,
    val remainingFreeSketches: Int,       // today's daily credits remaining
    val remainingMonthlySketches: Int,
    val extraCredits: Int,
    val totalAvailableCredits: Int,
    val rewardedAdsToday: Int = 0,
    val rewardedAdsRemaining: Int = CreditConfig.MAX_REWARDED_ADS_PER_DAY,
    /** Consecutive-day coloring streak (server-tracked, follows the account). */
    val streakCurrent: Int = 0,
    val streakBest: Int = 0,
    /** True only on the fetch that advanced the streak to a new day — drives a
     *  one-time Home celebration. Not persisted (transient per response). */
    val streakAdvancedToday: Boolean = false,
    // Parent controls — synced to Firestore, returned with every quota fetch.
    val parentDailySketchLimit: Int? = null,   // null = unlimited
    val parentAllowFreeText: Boolean = true,
    val parentSessionLimitMinutes: Int? = null  // null = off
) {
    val isPremium: Boolean get() = plan == "pro" && subscriptionActive

    companion object {
        val UNKNOWN = UserQuota(
            plan = "free",
            subscriptionActive = false,
            remainingFreeSketches = 0,
            remainingMonthlySketches = 0,
            extraCredits = 0,
            totalAvailableCredits = 0,
            rewardedAdsToday = 0,
            rewardedAdsRemaining = CreditConfig.MAX_REWARDED_ADS_PER_DAY
        )
    }
}
