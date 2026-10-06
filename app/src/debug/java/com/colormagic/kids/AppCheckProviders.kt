package com.colormagic.kids

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds → Debug provider. On first run Logcat prints a debug token;
 * register it under Firebase Console → App Check → Apps so local builds
 * aren't rejected. Lives in the debug source set because the debug App Check
 * library is a debugImplementation dependency.
 */
internal object AppCheckProviders {
    fun factory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}
