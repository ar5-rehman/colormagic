package com.colormagic.kids.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "onboarding_prefs")

/**
 * Remembers whether the user has already been through the first-run flow
 * (onboarding + the welcome paywall). Persisted locally, so it survives app
 * restarts but resets on uninstall/fresh-install — exactly "show once per
 * install".
 */
@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.onboardingDataStore

    /** True once the user has seen onboarding + the first-run paywall. */
    val completed: Flow<Boolean> = store.data.map { it[KEY_COMPLETED] ?: false }

    suspend fun setCompleted() {
        store.edit { it[KEY_COMPLETED] = true }
    }

    private companion object {
        val KEY_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
