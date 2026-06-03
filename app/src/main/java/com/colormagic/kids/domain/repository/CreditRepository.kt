package com.colormagic.kids.domain.repository

import com.colormagic.kids.domain.model.UserQuota
import kotlinx.coroutines.flow.Flow

/** Result of a rewarded-ad grant attempt. */
sealed interface RewardedAdGrantResult {
    data class Success(val creditsGranted: Int, val newBalance: Int, val adsRemaining: Int) :
        RewardedAdGrantResult
    data object DailyLimitReached : RewardedAdGrantResult
    data class Failed(val message: String) : RewardedAdGrantResult
}

/**
 * Manages the credit economy: daily grants, rewarded-ad grants, and local
 * pre-flight state so the UI never shows a stale credit count.
 */
interface CreditRepository {

    /** Live stream of locally-cached quota state. Updated after every server call. */
    val quotaFlow: Flow<UserQuota>

    /** Fetches the latest quota from the server and refreshes [quotaFlow]. */
    suspend fun refreshQuota(): Result<UserQuota>

    /**
     * Tells the backend that a rewarded ad completed. The backend validates
     * the daily cap and adds REWARDED_AD_CREDITS to extraCredits.
     */
    suspend fun grantRewardedAdCredits(): RewardedAdGrantResult

    /** Returns how many rewarded ads the user has watched today (local cache). */
    suspend fun rewardedAdsTodayLocal(): Int

    /** Updates the local cache after a successful server grant. */
    suspend fun updateLocalQuota(quota: UserQuota)
}
