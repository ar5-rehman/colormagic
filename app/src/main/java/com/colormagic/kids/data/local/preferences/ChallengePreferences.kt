package com.colormagic.kids.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.challengeDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "challenge_prefs")

@Singleton
class ChallengePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.challengeDataStore

    val lastChallengeDate: Flow<String> = store.data.map { it[KEY_LAST_DATE] ?: "" }
    val bestScore: Flow<Int> = store.data.map { it[KEY_BEST_SCORE] ?: 0 }
    val totalChallenges: Flow<Int> = store.data.map { it[KEY_TOTAL] ?: 0 }
    val totalStars: Flow<Int> = store.data.map { it[KEY_TOTAL_STARS] ?: 0 }
    val lastScore: Flow<Int> = store.data.map { it[KEY_LAST_SCORE] ?: 0 }
    val lastStars: Flow<Int> = store.data.map { it[KEY_LAST_STARS] ?: 0 }

    suspend fun hasCompletedToday(): Boolean {
        val last = store.data.first()[KEY_LAST_DATE] ?: ""
        return last == todayKey()
    }

    suspend fun saveResult(score: Int, stars: Int) {
        store.edit { prefs ->
            prefs[KEY_LAST_DATE] = todayKey()
            prefs[KEY_LAST_SCORE] = score
            prefs[KEY_LAST_STARS] = stars
            prefs[KEY_TOTAL] = (prefs[KEY_TOTAL] ?: 0) + 1
            prefs[KEY_TOTAL_STARS] = (prefs[KEY_TOTAL_STARS] ?: 0) + stars
            val prev = prefs[KEY_BEST_SCORE] ?: 0
            if (score > prev) prefs[KEY_BEST_SCORE] = score
        }
    }

    private fun todayKey(): String {
        val cal = java.util.Calendar.getInstance()
        return "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.MONTH)}-${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
    }

    private companion object {
        val KEY_LAST_DATE = stringPreferencesKey("challenge_last_date")
        val KEY_BEST_SCORE = intPreferencesKey("challenge_best_score")
        val KEY_TOTAL = intPreferencesKey("challenge_total")
        val KEY_TOTAL_STARS = intPreferencesKey("challenge_total_stars")
        val KEY_LAST_SCORE = intPreferencesKey("challenge_last_score")
        val KEY_LAST_STARS = intPreferencesKey("challenge_last_stars")
    }
}
