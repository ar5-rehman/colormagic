package com.colormagic.kids.domain.repository

import android.app.Activity
import com.colormagic.kids.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

// The single contract for "is the parent signed in, and how do we sign them
// in / out?" The presentation layer never talks to Credential Manager — it
// goes through this interface so swapping providers (Firebase Auth, custom
// backend, etc.) is a single-implementation change.
interface AuthRepository {
    val currentUser: StateFlow<UserProfile?>

    val isSignedIn: Boolean get() = currentUser.value != null

    /** Triggers the Sign in with Google flow. The activity is needed so
     *  CredentialManager can attach its bottom-sheet UI. */
    suspend fun signInWithGoogle(activity: Activity): Result<UserProfile>

    /** Clears the cached credential and resets [currentUser] to null. */
    suspend fun signOut()
}
