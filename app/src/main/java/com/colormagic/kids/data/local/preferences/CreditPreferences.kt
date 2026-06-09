package com.colormagic.kids.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.colormagic.kids.domain.model.UserQuota
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.creditDataStore: DataStore<Preferences> by preferencesDataStore(name = "credit_prefs")

@Singleton
class CreditPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private val store = context.creditDataStore

    private object Keys {
        val TOTAL_CREDITS = intPreferencesKey("total_credits")
        val PLAN = stringPreferencesKey("plan")
        val REWARDED_ADS_TODAY = intPreferencesKey("rewarded_ads_today")
        val REWARDED_ADS_DATE = stringPreferencesKey("rewarded_ads_date")   // "YYYY-MM-DD"
        val DAILY_CREDITS_DATE = stringPreferencesKey("daily_credits_date") // "YYYY-MM-DD"
        val DAILY_CREDITS_AVAILABLE = intPreferencesKey("daily_credits_available")
        val SUBSCRIPTION_ACTIVE = intPreferencesKey("subscription_active")  // 0 or 1
        val STREAK_CURRENT = intPreferencesKey("streak_current")
        val STREAK_BEST = intPreferencesKey("streak_best")
    }

    /** Emits the locally-cached total available credits. */
    val totalCreditsFlow: Flow<Int> = store.data.map { it[Keys.TOTAL_CREDITS] ?: 0 }

    val planFlow: Flow<String> = store.data.map { it[Keys.PLAN] ?: "free" }

    val isSubscriptionActiveFlow: Flow<Boolean> =
        store.data.map { (it[Keys.SUBSCRIPTION_ACTIVE] ?: 0) == 1 }

    /** Returns how many rewarded ads the user has watched today (local cache). */
    suspend fun rewardedAdsToday(): Int {
        val prefs = store.data.first()
        val date = prefs[Keys.REWARDED_ADS_DATE]
        return if (date == todayString()) prefs[Keys.REWARDED_ADS_TODAY] ?: 0 else 0
    }

    /**
     * Reconstructs the last cached quota, or null if the server has never
     * returned one on this install. Used so the credit pill can show the last
     * known balance instantly (and stop its loading shimmer) instead of
     * waiting on — or hanging forever after — a failed network call.
     */
    suspend fun cachedQuota(): UserQuota? {
        val prefs = store.data.first()
        val total = prefs[Keys.TOTAL_CREDITS] ?: return null // never cached yet
        val today = todayString()
        val daily = if (prefs[Keys.DAILY_CREDITS_DATE] == today) {
            prefs[Keys.DAILY_CREDITS_AVAILABLE] ?: 0
        } else 0
        val adsToday = if (prefs[Keys.REWARDED_ADS_DATE] == today) {
            prefs[Keys.REWARDED_ADS_TODAY] ?: 0
        } else 0
        return UserQuota(
            plan = prefs[Keys.PLAN] ?: "free",
            subscriptionActive = (prefs[Keys.SUBSCRIPTION_ACTIVE] ?: 0) == 1,
            remainingFreeSketches = daily,
            remainingMonthlySketches = 0,
            extraCredits = 0,
            totalAvailableCredits = total,
            rewardedAdsToday = adsToday,
            rewardedAdsRemaining = (MAX_REWARDED_ADS_PER_DAY - adsToday).coerceAtLeast(0),
            streakCurrent = prefs[Keys.STREAK_CURRENT] ?: 0,
            streakBest = prefs[Keys.STREAK_BEST] ?: 0
        )
    }

    /** Saves the full quota snapshot from the server to local cache. */
    suspend fun saveQuota(quota: UserQuota) {
        store.edit { prefs ->
            prefs[Keys.TOTAL_CREDITS] = quota.totalAvailableCredits
            prefs[Keys.PLAN] = quota.plan
            prefs[Keys.SUBSCRIPTION_ACTIVE] = if (quota.subscriptionActive) 1 else 0
            prefs[Keys.STREAK_CURRENT] = quota.streakCurrent
            prefs[Keys.STREAK_BEST] = quota.streakBest
            // Daily credits
            prefs[Keys.DAILY_CREDITS_DATE] = todayString()
            prefs[Keys.DAILY_CREDITS_AVAILABLE] = quota.remainingFreeSketches
            // Rewarded ads (server is authoritative on date-boundary resets)
            prefs[Keys.REWARDED_ADS_DATE] = todayString()
            prefs[Keys.REWARDED_ADS_TODAY] =
                (MAX_REWARDED_ADS_PER_DAY - quota.rewardedAdsRemaining).coerceAtLeast(0)
        }
    }

    /** Locally increments the rewarded-ad counter after a successful server grant. */
    suspend fun incrementRewardedAdsToday(newBalance: Int) {
        store.edit { prefs ->
            val date = prefs[Keys.REWARDED_ADS_DATE]
            val today = todayString()
            val current = if (date == today) prefs[Keys.REWARDED_ADS_TODAY] ?: 0 else 0
            prefs[Keys.REWARDED_ADS_DATE] = today
            prefs[Keys.REWARDED_ADS_TODAY] = current + 1
            prefs[Keys.TOTAL_CREDITS] = newBalance
        }
    }

    private fun todayString(): String = LocalDate.now().toString()

    companion object {
        private const val MAX_REWARDED_ADS_PER_DAY = 5
    }
}
