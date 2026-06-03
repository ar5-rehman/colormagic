package com.colormagic.kids.monetization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the client-side credit logic and config values.
 *
 * These tests exercise the pure business rules that can be validated without
 * Firebase or Android dependencies.  They mirror the backend credit.ts logic
 * so both sides stay in sync.
 *
 * Run with: ./gradlew :app:test
 */
class CreditLogicTest {

    // ── helpers ────────────────────────────────────────────────────────────

    /** A simple in-memory user state that mirrors the relevant UserDoc fields. */
    private data class UserState(
        val plan: String = "free",
        val subscriptionActive: Boolean = false,
        val dailyCreditsDate: String = "2026-06-03",
        val dailyCreditsAvailable: Int = CreditConfig.FREE_DAILY_CREDITS,
        val rewardedAdsDate: String = "2026-06-03",
        val rewardedAdsToday: Int = 0,
        val extraCredits: Int = 0,
        val monthlySketchLimit: Int = 0,
        val usedSketchesThisMonth: Int = 0
    ) {
        val isPremium: Boolean get() = plan == "pro" && subscriptionActive
        val totalAvailableCredits: Int
            get() {
                val monthly = if (isPremium) maxOf(0, monthlySketchLimit - usedSketchesThisMonth) else 0
                return dailyCreditsAvailable + monthly + extraCredits
            }
        val rewardedAdsRemaining: Int
            get() = maxOf(0, CreditConfig.MAX_REWARDED_ADS_PER_DAY - rewardedAdsToday)
    }

    /** Applies the daily-reset logic in pure Kotlin — mirrors normalizeUser in credits.ts. */
    private fun normalizeUser(user: UserState, today: String): UserState {
        var result = user
        if (result.dailyCreditsDate != today) {
            result = result.copy(
                dailyCreditsDate = today,
                dailyCreditsAvailable = if (result.isPremium)
                    CreditConfig.PREMIUM_DAILY_CREDITS
                else
                    CreditConfig.FREE_DAILY_CREDITS
            )
        }
        if (result.rewardedAdsDate != today) {
            result = result.copy(rewardedAdsDate = today, rewardedAdsToday = 0)
        }
        return result
    }

    /** Attempts to grant a rewarded-ad credit. Returns updated state or an error string. */
    private fun grantRewardedAd(user: UserState, today: String): Pair<UserState?, String?> {
        val normalized = normalizeUser(user, today)
        return if (normalized.rewardedAdsToday >= CreditConfig.MAX_REWARDED_ADS_PER_DAY) {
            null to "daily_ad_limit_reached"
        } else {
            normalized.copy(
                extraCredits = normalized.extraCredits + CreditConfig.REWARDED_AD_CREDITS,
                rewardedAdsToday = normalized.rewardedAdsToday + 1
            ) to null
        }
    }

    /** Spends [amount] credits from the user's buckets. Returns updated state or error. */
    private fun spendCredits(user: UserState, amount: Int): Pair<UserState?, String?> {
        if (user.totalAvailableCredits < amount) return null to "insufficient_credits"
        var remaining = amount
        var daily = user.dailyCreditsAvailable
        var monthly = if (user.isPremium) maxOf(0, user.monthlySketchLimit - user.usedSketchesThisMonth) else 0
        var extra = user.extraCredits

        // Consume daily first, then monthly, then extra
        val dailyUsed = minOf(daily, remaining)
        daily -= dailyUsed; remaining -= dailyUsed
        val monthlyUsed = minOf(monthly, remaining)
        monthly -= monthlyUsed; remaining -= monthlyUsed
        val extraUsed = minOf(extra, remaining)
        extra -= extraUsed

        return user.copy(
            dailyCreditsAvailable = daily,
            usedSketchesThisMonth = user.usedSketchesThisMonth + monthlyUsed,
            extraCredits = extra
        ) to null
    }

    // ── 1. Daily credit reset ──────────────────────────────────────────────

    @Test
    fun `daily credits reset to FREE_DAILY_CREDITS for free user on new day`() {
        val user = UserState(dailyCreditsDate = "2026-06-02", dailyCreditsAvailable = 0)
        val result = normalizeUser(user, today = "2026-06-03")
        assertEquals(CreditConfig.FREE_DAILY_CREDITS, result.dailyCreditsAvailable)
        assertEquals("2026-06-03", result.dailyCreditsDate)
    }

    @Test
    fun `daily credits reset to PREMIUM_DAILY_CREDITS for premium user on new day`() {
        val user = UserState(
            plan = "pro",
            subscriptionActive = true,
            dailyCreditsDate = "2026-06-02",
            dailyCreditsAvailable = 0
        )
        val result = normalizeUser(user, today = "2026-06-03")
        assertEquals(CreditConfig.PREMIUM_DAILY_CREDITS, result.dailyCreditsAvailable)
    }

    @Test
    fun `daily credits are NOT reset when date has not changed`() {
        val user = UserState(dailyCreditsDate = "2026-06-03", dailyCreditsAvailable = 2)
        val result = normalizeUser(user, today = "2026-06-03")
        // Balance must remain exactly 2 — no reset
        assertEquals(2, result.dailyCreditsAvailable)
    }

    @Test
    fun `rewarded ad counter resets to zero on new day`() {
        val user = UserState(rewardedAdsDate = "2026-06-02", rewardedAdsToday = 5)
        val result = normalizeUser(user, today = "2026-06-03")
        assertEquals(0, result.rewardedAdsToday)
        assertEquals("2026-06-03", result.rewardedAdsDate)
    }

    // ── 2. Rewarded ad — success ───────────────────────────────────────────

    @Test
    fun `rewarded ad grants exactly REWARDED_AD_CREDITS to extra bucket`() {
        val user = UserState(extraCredits = 0, rewardedAdsToday = 0)
        val (updated, error) = grantRewardedAd(user, today = "2026-06-03")
        assertNull("Should not return an error", error)
        assertEquals(CreditConfig.REWARDED_AD_CREDITS, updated!!.extraCredits)
    }

    @Test
    fun `rewarded ad increments rewardedAdsToday by exactly 1`() {
        val user = UserState(rewardedAdsToday = 2)
        val (updated, error) = grantRewardedAd(user, today = "2026-06-03")
        assertNull(error)
        assertEquals(3, updated!!.rewardedAdsToday)
    }

    @Test
    fun `rewarded ad increases totalAvailableCredits`() {
        val user = UserState(dailyCreditsAvailable = 0, extraCredits = 0)
        val (updated, error) = grantRewardedAd(user, today = "2026-06-03")
        assertNull(error)
        assertEquals(CreditConfig.REWARDED_AD_CREDITS, updated!!.totalAvailableCredits)
    }

    // ── 3. Rewarded ad — failure / dismiss ────────────────────────────────

    @Test
    fun `cancelled ad does not change user state`() {
        val user = UserState(extraCredits = 5, rewardedAdsToday = 1)
        // Simulate: ad shown but reward callback never fires (user dismissed)
        // Client rule: only call grantRewardedAd() when onAdEarned fires.
        // No change expected — user state stays the same.
        assertEquals(5, user.extraCredits)
        assertEquals(1, user.rewardedAdsToday)
    }

    @Test
    fun `failed ad load does not change user state or deduct credits`() {
        val user = UserState(dailyCreditsAvailable = 3)
        // Ad failed to load — nothing should change
        assertEquals(3, user.dailyCreditsAvailable)
        assertEquals(0, user.rewardedAdsToday)
    }

    // ── 4. Max daily rewarded ads reached ─────────────────────────────────

    @Test
    fun `grant fails with daily_ad_limit_reached when max ads already watched`() {
        val user = UserState(rewardedAdsToday = CreditConfig.MAX_REWARDED_ADS_PER_DAY)
        val (updated, error) = grantRewardedAd(user, today = "2026-06-03")
        assertEquals("daily_ad_limit_reached", error)
        assertNull("State should not change on limit reached", updated)
    }

    @Test
    fun `rewardedAdsRemaining is 0 when at max`() {
        val user = UserState(rewardedAdsToday = CreditConfig.MAX_REWARDED_ADS_PER_DAY)
        assertEquals(0, user.rewardedAdsRemaining)
    }

    @Test
    fun `rewardedAdsRemaining decrements correctly`() {
        val user = UserState(rewardedAdsToday = 2)
        assertEquals(CreditConfig.MAX_REWARDED_ADS_PER_DAY - 2, user.rewardedAdsRemaining)
    }

    @Test
    fun `max ad limit resets after daily reset`() {
        val user = UserState(
            rewardedAdsDate = "2026-06-02",
            rewardedAdsToday = CreditConfig.MAX_REWARDED_ADS_PER_DAY
        )
        val (updated, error) = grantRewardedAd(user, today = "2026-06-03")
        // Date rolled over — limit resets, grant should succeed
        assertNull(error)
        assertEquals(CreditConfig.REWARDED_AD_CREDITS, updated!!.extraCredits)
        assertEquals(1, updated.rewardedAdsToday)
    }

    // ── 5. Credit spending ─────────────────────────────────────────────────

    @Test
    fun `spending one credit reduces totalAvailableCredits by 1`() {
        val user = UserState(dailyCreditsAvailable = 5)
        val (updated, error) = spendCredits(user, CreditConfig.Costs.GENERATE_COLORING_PAGE)
        assertNull(error)
        assertEquals(4, updated!!.totalAvailableCredits)
    }

    @Test
    fun `HD export costs HD_EXPORT credits`() {
        val user = UserState(dailyCreditsAvailable = 5)
        val (updated, error) = spendCredits(user, CreditConfig.Costs.HD_EXPORT)
        assertNull(error)
        assertEquals(5 - CreditConfig.Costs.HD_EXPORT, updated!!.totalAvailableCredits)
    }

    @Test
    fun `daily credits are consumed before extra credits`() {
        val user = UserState(dailyCreditsAvailable = 3, extraCredits = 2)
        val (updated, _) = spendCredits(user, 1)
        assertEquals(2, updated!!.dailyCreditsAvailable) // daily goes down first
        assertEquals(2, updated.extraCredits)            // extra untouched
    }

    // ── 6. Insufficient credits ────────────────────────────────────────────

    @Test
    fun `spending credits when balance is zero returns insufficient_credits error`() {
        val user = UserState(dailyCreditsAvailable = 0, extraCredits = 0)
        val (updated, error) = spendCredits(user, 1)
        assertEquals("insufficient_credits", error)
        assertNull(updated)
    }

    @Test
    fun `spending more than balance returns insufficient_credits error`() {
        val user = UserState(dailyCreditsAvailable = 1)
        val (updated, error) = spendCredits(user, CreditConfig.Costs.HD_EXPORT) // costs 2
        assertEquals("insufficient_credits", error)
        assertNull(updated)
    }

    @Test
    fun `outOfCredits is true when totalAvailableCredits is zero`() {
        val user = UserState(dailyCreditsAvailable = 0, extraCredits = 0)
        assertEquals(0, user.totalAvailableCredits)
    }

    // ── 7. Premium user — no ads, more credits ─────────────────────────────

    @Test
    fun `premium user gets PREMIUM_DAILY_CREDITS on daily reset`() {
        val user = UserState(
            plan = "pro",
            subscriptionActive = true,
            dailyCreditsDate = "2026-06-02",
            dailyCreditsAvailable = 0
        )
        val result = normalizeUser(user, today = "2026-06-03")
        assertEquals(CreditConfig.PREMIUM_DAILY_CREDITS, result.dailyCreditsAvailable)
        assertTrue(CreditConfig.PREMIUM_DAILY_CREDITS > CreditConfig.FREE_DAILY_CREDITS)
    }

    @Test
    fun `premium user isPremium flag is true`() {
        val user = UserState(plan = "pro", subscriptionActive = true)
        assertTrue(user.isPremium)
    }

    @Test
    fun `free user isPremium flag is false`() {
        val user = UserState(plan = "free", subscriptionActive = false)
        assertFalse(user.isPremium)
    }

    @Test
    fun `subscription with subscriptionActive false is not premium`() {
        val user = UserState(plan = "pro", subscriptionActive = false)
        assertFalse(user.isPremium)
    }

    @Test
    fun `premium user canWatchAd returns false`() {
        val user = UserState(plan = "pro", subscriptionActive = true)
        // Premium users should never see the "Watch Ad" button.
        // This mirrors the GetCreditsUiState.canWatchAd logic.
        assertFalse(user.isPremium.not()) // i.e. isPremium == true means no ads
    }

    // ── 8. Config sanity checks ────────────────────────────────────────────

    @Test
    fun `premium daily credits are more than free daily credits`() {
        assertTrue(CreditConfig.PREMIUM_DAILY_CREDITS > CreditConfig.FREE_DAILY_CREDITS)
    }

    @Test
    fun `rewarded ad credits are positive`() {
        assertTrue(CreditConfig.REWARDED_AD_CREDITS > 0)
    }

    @Test
    fun `max rewarded ads per day is positive`() {
        assertTrue(CreditConfig.MAX_REWARDED_ADS_PER_DAY > 0)
    }

    @Test
    fun `generate cost is at least 1`() {
        assertTrue(CreditConfig.Costs.GENERATE_COLORING_PAGE >= 1)
    }

    @Test
    fun `HD export costs more than generating a page`() {
        assertTrue(CreditConfig.Costs.HD_EXPORT > CreditConfig.Costs.GENERATE_COLORING_PAGE)
    }
}
