package com.colormagic.kids.data.telemetry

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import kotlinx.coroutines.tasks.await
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

    /**
     * Core funnel event (see [AnalyticsEvent]). Param values must be counts,
     * flags or short stable codes — NEVER prompts, names or other free text.
     */
    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap()) {
        analytics.logEvent(eventName) {
            params.forEach { (key, value) ->
                when (value) {
                    is Int -> param(key, value.toLong())
                    is Long -> param(key, value)
                    is Double -> param(key, value)
                    is Boolean -> param(key, if (value) 1L else 0L)
                    else -> param(key, value.toString())
                }
            }
        }
    }

    /** Firebase Analytics instance id — sent with purchases so the server can
     *  attribute subscription renew/cancel events (Measurement Protocol). */
    suspend fun appInstanceId(): String? =
        runCatching { analytics.appInstanceId.await() }.getOrNull()
}

/** The core funnel / retention events, in one place so names never drift. */
object AnalyticsEvent {
    const val APP_OPEN = "app_open"
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val SKETCH_GENERATION_STARTED = "sketch_generation_started"
    const val SKETCH_GENERATED = "sketch_generated"
    const val SKETCH_GENERATION_FAILED = "sketch_generation_failed"
    const val COLORING_STARTED = "coloring_started"
    const val ARTWORK_SAVED = "artwork_saved"
    const val REWARDED_AD_WATCHED = "rewarded_ad_watched"
    const val SUBSCRIPTION_SCREEN_VIEWED = "subscription_screen_viewed"
    const val SUBSCRIPTION_STARTED = "subscription_started"
    const val CREDIT_PACK_PURCHASED = "credit_pack_purchased"
    // subscription_renewed / subscription_cancelled are sent by the SERVER
    // (Play RTDN → Measurement Protocol) — the app never sees those moments.
}
