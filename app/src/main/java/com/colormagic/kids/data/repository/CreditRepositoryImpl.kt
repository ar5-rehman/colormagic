package com.colormagic.kids.data.repository

import com.colormagic.kids.data.local.preferences.CreditPreferences
import com.colormagic.kids.data.telemetry.AppTelemetry
import com.colormagic.kids.domain.model.UserQuota
import com.colormagic.kids.domain.repository.CreditRepository
import com.colormagic.kids.domain.repository.RewardedAdGrantResult
import com.colormagic.kids.monetization.CreditConfig
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
    private val creditPrefs: CreditPreferences,
    private val telemetry: AppTelemetry
) : CreditRepository {

    private val _quotaFlow = MutableStateFlow(UserQuota.UNKNOWN)
    override val quotaFlow: Flow<UserQuota> = _quotaFlow.asStateFlow()

    override suspend fun refreshQuota(): Result<UserQuota> = runCatching {
        val response = functions
            .getHttpsCallable(FN_USER_QUOTA)
            .call()
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = response.getData() as Map<String, Any?>
        val quota = UserQuota(
            plan = data["plan"] as? String ?: "free",
            subscriptionActive = data["subscriptionActive"] as? Boolean ?: false,
            remainingFreeSketches = (data["remainingFreeSketches"] as? Number)?.toInt() ?: 0,
            remainingMonthlySketches = (data["remainingMonthlySketches"] as? Number)?.toInt() ?: 0,
            extraCredits = (data["extraCredits"] as? Number)?.toInt() ?: 0,
            totalAvailableCredits = (data["totalAvailableCredits"] as? Number)?.toInt() ?: 0,
            rewardedAdsToday = (data["rewardedAdsToday"] as? Number)?.toInt() ?: 0,
            rewardedAdsRemaining = (data["rewardedAdsRemaining"] as? Number)?.toInt()
                ?: CreditConfig.MAX_REWARDED_ADS_PER_DAY
        )
        updateLocalQuota(quota)
        quota
    }.onFailure { telemetry.recordNonFatal(it) }

    override suspend fun grantRewardedAdCredits(): RewardedAdGrantResult {
        telemetry.logCreditEvent("rewarded_ad_requested")
        return runCatching {
            val response = functions
                .getHttpsCallable(FN_GRANT_REWARDED_AD_CREDITS)
                .call()
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = response.getData() as Map<String, Any?>
            val success = data["success"] as? Boolean ?: false
            val errorCode = data["errorCode"] as? String

            when {
                errorCode == "daily_ad_limit_reached" ->
                    RewardedAdGrantResult.DailyLimitReached

                success -> {
                    val newBalance = (data["newBalance"] as? Number)?.toInt() ?: 0
                    val adsRemaining = (data["rewardedAdsRemaining"] as? Number)?.toInt() ?: 0
                    creditPrefs.incrementRewardedAdsToday(newBalance)
                    _quotaFlow.value = _quotaFlow.value.copy(
                        totalAvailableCredits = newBalance,
                        rewardedAdsRemaining = adsRemaining,
                        rewardedAdsToday = _quotaFlow.value.rewardedAdsToday + 1
                    )
                    telemetry.logCreditEvent("credits_granted", CreditConfig.REWARDED_AD_CREDITS)
                    RewardedAdGrantResult.Success(
                        creditsGranted = CreditConfig.REWARDED_AD_CREDITS,
                        newBalance = newBalance,
                        adsRemaining = adsRemaining
                    )
                }

                else -> RewardedAdGrantResult.Failed("Unexpected server response")
            }
        }.getOrElse { e ->
            telemetry.recordNonFatal(e)
            RewardedAdGrantResult.Failed(e.message ?: "Network error")
        }
    }

    override suspend fun rewardedAdsTodayLocal(): Int = creditPrefs.rewardedAdsToday()

    override suspend fun updateLocalQuota(quota: UserQuota) {
        _quotaFlow.value = quota
        creditPrefs.saveQuota(quota)
    }

    private companion object {
        const val FN_USER_QUOTA = "userQuota"
        const val FN_GRANT_REWARDED_AD_CREDITS = "grantRewardedAdCredits"
    }
}
