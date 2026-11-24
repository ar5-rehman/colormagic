package com.colormagic.kids.presentation.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

/**
 * Google Play in-app review, shown to the PARENT, never to the child.
 *
 * - Children can't (and shouldn't) rate apps; the review is posted from the device's Google account, which
 *   belongs to the parent. So we only ask behind the parent gate: when the parent opens the Parent Area, or
 *   right after a purchase, once the child has actually made pictures with the app.
 * - Play policy: no "Do you like the app?" question first and no rewards. Asking only happy users
 *   (review gating) is forbidden. We only choose a good moment.
 * - Google decides whether the card really appears (it has its own quota), so we ask rarely: after
 *   1, 8 and 25 coloring pages made, at least 30 days apart, at most 3 times.
 */
object AppReview {
    private const val PREFS = "app_review"
    /** Pages needed before the 1st, 2nd and 3rd ask. */
    private val ASK_AT = listOf(1, 8, 25)
    private const val MIN_GAP_MS = 30L * 24 * 3600 * 1000

    /** A coloring page was created successfully. */
    fun recordSketch(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putInt("sketches", sp.getInt("sketches", 0) + 1).apply()
    }

    /** Call when a parent is using the phone (Parent Area after the gate, purchase success). */
    suspend fun maybeAsk(context: Context) {
        val activity = context.findActivity() ?: return
        val sp = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val asks = sp.getInt("asks", 0)
        if (asks >= ASK_AT.size) return
        if (sp.getInt("sketches", 0) < ASK_AT[asks]) return
        if (System.currentTimeMillis() - sp.getLong("lastAsk", 0L) < MIN_GAP_MS) return

        delay(1000) // let the screen settle first
        if (activity.isFinishing || activity.isDestroyed) return
        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            val info = manager.requestReviewFlow().await()
            sp.edit().putInt("asks", asks + 1).putLong("lastAsk", System.currentTimeMillis()).apply()
            manager.launchReviewFlow(activity, info).await()
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
