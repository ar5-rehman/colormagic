package com.colormagic.kids

import android.app.Application
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.Firebase
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ColorMagicKidsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebaseAppCheck()
    }

    /**
     * App Check proves to the Firebase backend that requests come from this
     * genuine app build — not a script replaying the API. Every Cloud
     * Function / Firestore / Storage call carries an App Check token.
     *
     *  • Release  → Play Integrity (hardware-backed attestation).
     *  • Debug    → Debug provider. On first run Logcat prints a debug token;
     *               register it under Firebase Console → App Check → Apps so
     *               local builds aren't rejected.
     */
    private fun initFirebaseAppCheck() {
        Firebase.initialize(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        )
    }
}
