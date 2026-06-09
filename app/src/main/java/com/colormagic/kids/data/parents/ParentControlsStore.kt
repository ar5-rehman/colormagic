package com.colormagic.kids.data.parents

import android.content.Context
import com.colormagic.kids.domain.model.ParentControls
import com.colormagic.kids.domain.model.SketchLimit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

// SharedPreferences-backed store for the parent-control toggles +
// today's sketch counter. Exposes a StateFlow so the relevant screens
// (Create, Parents) update reactively — no manual refresh needed.
//
// Counter semantics:
//   • Every completed sketch calls [recordSketch()].
//   • On the first record of a new day, the counter resets to 1.
//   • Day boundaries are local-timezone calendar days (not UTC), which is
//     what a parent intuitively expects from "10 sketches per day".
@Singleton
class ParentControlsStore @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<ParentControls> = _state.asStateFlow()

    fun setAllowFreeText(allow: Boolean) {
        prefs.edit().putBoolean(K_ALLOW_FREE_TEXT, allow).apply()
        _state.update { it.copy(allowFreeText = allow) }
    }

    fun setDailyLimit(limit: SketchLimit) {
        // Stored as the per-day int (-1 = Unlimited) so any custom number works.
        prefs.edit().putInt(K_DAILY_LIMIT_N, limit.perDay ?: UNLIMITED).apply()
        _state.update { it.copy(dailyLimit = limit) }
    }

    /** Sets the per-session screen-time cap in minutes; null turns it off. */
    fun setSessionLimit(minutes: Int?) {
        prefs.edit().putInt(K_SESSION_LIMIT, minutes ?: SESSION_OFF).apply()
        _state.update { it.copy(sessionLimitMinutes = minutes) }
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

    private fun load(): ParentControls {
        val allowFreeText = prefs.getBoolean(K_ALLOW_FREE_TEXT, true)
        val limitPerDay = prefs.getInt(K_DAILY_LIMIT_N, UNLIMITED)
        val limit = if (limitPerDay < 0) SketchLimit.Unlimited else SketchLimit(limitPerDay)
        val savedDay = prefs.getLong(K_DAY_STAMP, 0L)
        val today = todayStamp()
        // If the persisted counter is from a previous day, expose 0 — the
        // first new sketch will write the rollover atomically.
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
        const val K_DAILY_LIMIT_N = "daily_limit_per_day" // Int; -1 = Unlimited
        const val UNLIMITED = -1
        const val K_TODAY_COUNT = "today_count"
        const val K_DAY_STAMP = "day_stamp"
        const val K_SESSION_LIMIT = "session_limit_minutes"
        const val SESSION_OFF = -1
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
