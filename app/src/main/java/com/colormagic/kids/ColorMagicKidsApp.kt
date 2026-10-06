package com.colormagic.kids

import android.app.Application
import com.google.firebase.appcheck.appCheck
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
     * The provider is picked per build type (see AppCheckProviders in the
     * debug / release source sets): Debug provider for debug, Play Integrity
     * for release.
     */
    private fun initFirebaseAppCheck() {
        Firebase.initialize(this)
        Firebase.appCheck.installAppCheckProviderFactory(AppCheckProviders.factory())
    }
}
