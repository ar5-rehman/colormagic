package com.colormagic.kids.monetization

/**
 * Central credit economy configuration. Mirrors the backend config.ts values
 * so the UI can pre-check limits without a network round-trip. When changing
 * these values, update both this file and functions/src/config.ts.
 */
object CreditConfig {
    const val FREE_DAILY_CREDITS = 1
    const val REWARDED_AD_CREDITS = 3
    const val MAX_REWARDED_ADS_PER_DAY = 5
    const val PREMIUM_DAILY_CREDITS = 30
    const val PREMIUM_MONTHLY_PRICE_USD = 4.99

    /** AdMob rewarded ad unit ID — replace with the real unit ID from the AdMob console. */
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // test ID

    object Costs {
        // Only generation costs credits. Saving/exporting artwork to the phone
        // gallery is ALWAYS free for every user — no HD-export or watermark fee.
        const val GENERATE_COLORING_PAGE = 1
        const val PREMIUM_STYLE = 2
    }
}
