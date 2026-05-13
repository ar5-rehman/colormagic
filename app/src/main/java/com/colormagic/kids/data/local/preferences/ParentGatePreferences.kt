package com.colormagic.kids.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Persists a single boolean: has the parent opted into requiring biometric
// authentication before showing the Parent Area? Backed by SharedPreferences
// because it's a single flag — no need for a DataStore here.
@Singleton
class ParentGatePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isGateEnabled: Boolean
        get() = prefs.getBoolean(KEY_GATE_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_GATE_ENABLED, value).apply()
        }

    private companion object {
        const val PREFS_NAME = "parent_gate_prefs"
        const val KEY_GATE_ENABLED = "gate_enabled"
    }
}
