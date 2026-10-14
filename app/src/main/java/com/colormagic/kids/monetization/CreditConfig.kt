package com.colormagic.kids.monetization

import com.colormagic.kids.BuildConfig

/**
 * Central credit economy configuration. Mirrors the backend config.ts values
 * so the UI can pre-check limits without a network round-trip. When changing
 * these values, update both this file and functions/src/config.ts.
 */
object CreditConfig {
    // Free tier: no ongoing daily credit. A NEW user (first account on a phone)
    // gets WELCOME_CREDITS_PER_DAY on each of the first WELCOME_DAYS days; after
    // that, credits come from rewarded ads or purchases. Keep in sync with
    // functions/src/config.ts.
    const val FREE_DAILY_CREDITS = 0
    const val WELCOME_DAYS = 3
    const val WELCOME_CREDITS_PER_DAY = 1
    const val REWARDED_AD_CREDITS = 2
    const val MAX_REWARDED_ADS_PER_DAY = 3
    const val PREMIUM_DAILY_CREDITS = 30
    const val PREMIUM_MONTHLY_PRICE_USD = 7.99

    /** Real AdMob rewarded ad unit (release builds). */
    private const val REWARDED_AD_UNIT_ID_LIVE = "ca-app-pub-3327131790118517/1980142489"

    /** Google's public test rewarded unit — debug builds use it so we never
     *  tap our own live ads (AdMob suspends accounts for that). */
    private const val REWARDED_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/5224354917"

    val REWARDED_AD_UNIT_ID: String =
        if (BuildConfig.DEBUG) REWARDED_AD_UNIT_ID_TEST else REWARDED_AD_UNIT_ID_LIVE

    /**
     * When true, rewarded credits are granted by AdMob Server-Side Verification
     * (the backend admobSsvCallback function), NOT by the client callable. This
     * is the most tamper-proof path. Keep in sync with USE_ADMOB_SSV in the
     * backend config.ts. Requires SSV configured on the REAL rewarded ad unit
     * in the AdMob console (Server-side verification → callback URL = the
     * admobSsvCallback function URL). Google's test ad units never send SSV
     * callbacks, so with test IDs rewards won't arrive — set false (here AND
     * in config.ts) only for local testing with test ads.
     */
    const val USE_ADMOB_SSV = false  // see functions/src/config.ts

    object Costs {
        // Only generation costs credits. Saving/exporting artwork to the phone
        // gallery is ALWAYS free for every user — no HD-export or watermark fee.
        const val GENERATE_COLORING_PAGE = 1
        const val PREMIUM_STYLE = 2
    }
}
