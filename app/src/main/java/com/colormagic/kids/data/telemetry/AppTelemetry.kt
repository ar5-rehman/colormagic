package com.colormagic.kids.data.telemetry

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import javax.inject.Inject
import javax.inject.Singleton

// Thin façade over Firebase Crashlytics + Analytics. Centralising it means
// (1) the rest of the app doesn't import Firebase directly for telemetry and
// (2) we never accidentally log child-identifiable data — every event goes
// through one of these typed helpers.
@Singleton
class AppTelemetry @Inject constructor() {

    private val analytics: FirebaseAnalytics = Firebase.analytics
    private val crashlytics: FirebaseCrashlytics = Firebase.crashlytics

    /** A caught exception that didn't crash the app — generation failure,
     *  billing error, etc. The throwable's message must not contain the
     *  child's prompt or any other free-text PII. */
    fun recordNonFatal(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    /** Tags the active session with the anonymous Firebase uid. Lets us
     *  cross-reference a Crashlytics report with the user doc in Firestore
     *  without ever sending an email or device id. */
    fun setUserId(uid: String?) {
        crashlytics.setUserId(uid.orEmpty())
        analytics.setUserId(uid)
    }

    /** Screen view — drives the "engagement by screen" report. The name
     *  should be a short stable identifier, not a user-facing label. */
    fun logScreenView(screenName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
    }

    /** A sketch generation finished. [outcome] is one of: success, no_credits,
     *  rejected, failed. Never include the prompt text. */
    fun logSketchGenerated(outcome: String) {
        analytics.logEvent("sketch_generated") {
            param("outcome", outcome)
        }
    }

    /** A Play Billing purchase resolved. [outcome] is one of: success,
     *  cancelled, pending, failed. */
    fun logPurchase(productId: String, outcome: String) {
        analytics.logEvent("purchase_flow") {
            param("product_id", productId)
            param("outcome", outcome)
        }
    }

    /**
     * Credit economy events. [eventName] must be one of the defined analytics
     * event names (see the spec in the monetisation plan):
     *   credits_screen_opened, rewarded_ad_requested, rewarded_ad_loaded,
     *   rewarded_ad_completed, rewarded_ad_failed, credits_granted,
     *   credits_spent, low_credits_modal_shown, premium_screen_opened,
     *   subscription_started, subscription_restored, subscription_cancelled.
     */
    fun logCreditEvent(eventName: String, amount: Int = 0) {
        analytics.logEvent(eventName) {
            if (amount > 0) param("amount", amount.toLong())
        }
    }

    /** A generic, parameter-less app event (e.g. feedback_submitted). The name
     *  must be a stable identifier and must never contain free-text PII. */
    fun logAppEvent(eventName: String) {
        analytics.logEvent(eventName) {}
    }
}
