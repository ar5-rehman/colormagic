package com.colormagic.kids.data.parents

import android.content.Context
import android.util.Log
import com.colormagic.kids.domain.model.ParentControls
import com.colormagic.kids.domain.model.SketchLimit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parent-control store that keeps settings in both local SharedPreferences
 * (for instant reads + offline) and Firestore (for cross-device sync when
 * signed in with Google).
 *
 * Write path: local first (instant UI), then fire-and-forget to server.
 * Read path: load from local on cold start, then overwrite with server
 *            values when [seedFromServer] is called (after quota fetch).
 *
 * Guest users: settings save locally + to their anonymous Firestore doc.
 * If they sign out, the anonymous doc is deleted and settings are lost —
 * the UI warns them about this.
 */
@Singleton
class ParentControlsStore @Inject constructor(
    @ApplicationContext context: Context,
    private val functions: FirebaseFunctions,
    private val firebaseAuth: FirebaseAuth
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<ParentControls> = _state.asStateFlow()

    fun setAllowFreeText(allow: Boolean) {
        prefs.edit().putBoolean(K_ALLOW_FREE_TEXT, allow).apply()
        _state.update { it.copy(allowFreeText = allow) }
        syncToServer(allowFreeText = allow)
    }

    fun setDailyLimit(limit: SketchLimit) {
        prefs.edit().putInt(K_DAILY_LIMIT_N, limit.perDay ?: UNLIMITED).apply()
        _state.update { it.copy(dailyLimit = limit) }
        syncToServer(dailySketchLimit = limit.perDay)
    }

    /** Sets the per-session screen-time cap in minutes; null turns it off. */
    fun setSessionLimit(minutes: Int?) {
        prefs.edit().putInt(K_SESSION_LIMIT, minutes ?: SESSION_OFF).apply()
        _state.update { it.copy(sessionLimitMinutes = minutes) }
        syncToServer(sessionLimitMinutes = minutes)
    }

    /** Records one successful sketch toward today's counter, rolling the
     *  counter over to 1 if the previous use was on a different day. */
    fun recordSketch() {
        val today = todayStamp()
        val current = _state.value
        val (count, day) = if (current.dayStamp != today) 1 to today
        else current.sketchesToday + 1 to current.dayStamp
        prefs.edit()
            .putInt(K_TODAY_COUNT, count)
            .putLong(K_DAY_STAMP, day)
            .apply()
        _state.update { it.copy(sketchesToday = count, dayStamp = day) }
    }

    /**
     * Called after the userQuota response arrives — seeds local state with
     * server values so settings sync across devices. Server wins over local
     * for the synced fields (limit, free-text, session time). The daily
     * sketch counter stays local (it's per-device by design).
     */
    fun seedFromServer(
        dailySketchLimit: Int?,   // null = unlimited
        allowFreeText: Boolean,
        sessionLimitMinutes: Int? // null = off
    ) {
        val limit = if (dailySketchLimit == null) SketchLimit.Unlimited
                    else SketchLimit(dailySketchLimit)

        // Persist to local prefs so offline reads stay current.
        prefs.edit()
            .putInt(K_DAILY_LIMIT_N, dailySketchLimit ?: UNLIMITED)
            .putBoolean(K_ALLOW_FREE_TEXT, allowFreeText)
            .putInt(K_SESSION_LIMIT, sessionLimitMinutes ?: SESSION_OFF)
            .apply()

        _state.update {
            it.copy(
                dailyLimit = limit,
                allowFreeText = allowFreeText,
                sessionLimitMinutes = sessionLimitMinutes
            )
        }
    }

    // ── Private helpers ──────────────────────────────────────────────

    /** Fire-and-forget sync to Firestore via the saveParentControls callable. */
    private fun syncToServer(
        dailySketchLimit: Int? = SKIP_INT,
        allowFreeText: Boolean? = null,
        sessionLimitMinutes: Int? = SKIP_INT
    ) {
        if (firebaseAuth.currentUser == null) return
        scope.launch {
            try {
                val params = mutableMapOf<String, Any?>()
                if (dailySketchLimit != SKIP_INT) params["dailySketchLimit"] = dailySketchLimit
                if (allowFreeText != null) params["allowFreeText"] = allowFreeText
                if (sessionLimitMinutes != SKIP_INT) params["sessionLimitMinutes"] = sessionLimitMinutes
                if (params.isNotEmpty()) {
                    functions.getHttpsCallable(FN_SAVE_PARENT_CONTROLS)
                        .call(params)
                        .await()
                }
            } catch (e: Exception) {
                // Best-effort: local save already succeeded, server sync can
                // retry on the next setting change or app open.
                Log.w("ParentControlsStore", "Server sync failed", e)
            }
        }
    }

    private fun load(): ParentControls {
        val allowFreeText = prefs.getBoolean(K_ALLOW_FREE_TEXT, true)
        val limitPerDay = prefs.getInt(K_DAILY_LIMIT_N, UNLIMITED)
        val limit = if (limitPerDay < 0) SketchLimit.Unlimited else SketchLimit(limitPerDay)
        val savedDay = prefs.getLong(K_DAY_STAMP, 0L)
        val today = todayStamp()
        val count = if (savedDay == today) prefs.getInt(K_TODAY_COUNT, 0) else 0
        val sessionLimit = prefs.getInt(K_SESSION_LIMIT, SESSION_OFF)
            .takeIf { it != SESSION_OFF }
        return ParentControls(
            allowFreeText = allowFreeText,
            dailyLimit = limit,
            sketchesToday = count,
            dayStamp = today,
            sessionLimitMinutes = sessionLimit
        )
    }

    /** Days since epoch in the device's local timezone. */
    private fun todayStamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / DAY_MS
    }

    private companion object {
        const val PREFS = "parent_controls"
        const val K_ALLOW_FREE_TEXT = "allow_free_text"
        const val K_DAILY_LIMIT_N = "daily_limit_per_day"
        const val UNLIMITED = -1
        const val K_TODAY_COUNT = "today_count"
        const val K_DAY_STAMP = "day_stamp"
        const val K_SESSION_LIMIT = "session_limit_minutes"
        const val SESSION_OFF = -1
        const val DAY_MS = 24L * 60L * 60L * 1000L
        const val FN_SAVE_PARENT_CONTROLS = "saveParentControls"
        /** Sentinel meaning "this field wasn't passed to syncToServer". We can't
         *  use null because null is a valid value (= unlimited / off). */
        const val SKIP_INT = Int.MIN_VALUE
    }
}
