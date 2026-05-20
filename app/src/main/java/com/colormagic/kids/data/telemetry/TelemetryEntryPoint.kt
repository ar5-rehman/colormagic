package com.colormagic.kids.data.telemetry

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// Lets non-Hilt code (composables, NavController callbacks) pull the singleton
// AppTelemetry from the Hilt graph without us having to thread it through
// every screen's ViewModel. Use sparingly — ViewModel injection is still the
// preferred path for screens that already exist.
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TelemetryEntryPoint {
    fun appTelemetry(): AppTelemetry
}

fun Context.appTelemetry(): AppTelemetry =
    EntryPointAccessors.fromApplication(this, TelemetryEntryPoint::class.java).appTelemetry()
