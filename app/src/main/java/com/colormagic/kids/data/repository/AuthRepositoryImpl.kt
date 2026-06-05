package com.colormagic.kids.data.repository

import android.util.Log
import com.colormagic.kids.data.di.ApplicationScope
import com.colormagic.kids.data.telemetry.AppTelemetry
import com.colormagic.kids.domain.model.AuthUser
import com.colormagic.kids.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Firebase-backed implementation of [AuthRepository].
//
// Anonymous auth only. The OpenAI key, credit ledger and purchase
// verification all live on the Firebase backend; this class just makes sure
// the device has a verified Firebase identity to attach those calls to.
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val telemetry: AppTelemetry,
    @ApplicationScope private val appScope: CoroutineScope
) : AuthRepository {

    // Bridges FirebaseAuth's listener API into a hot StateFlow. SharingStarted
    // .Eagerly + the app-lifetime scope means the value is always current,
    // even before any screen starts collecting it. Each emission also tags
    // Crashlytics/Analytics with the current anonymous uid so future reports
    // join back to the right user doc in Firestore.
    override val authState: StateFlow<AuthUser?> =
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                val user = auth.currentUser?.toAuthUser()
                telemetry.setUserId(user?.uid)
                trySend(user)
            }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }.stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            initialValue = firebaseAuth.currentUser?.toAuthUser()
                .also { telemetry.setUserId(it?.uid) }
        )

    override val currentUser: AuthUser?
        get() = firebaseAuth.currentUser?.toAuthUser()

    override suspend fun ensureSignedIn(): Result<AuthUser> = runCatching {
        // Already signed in (returning user) → reuse the existing uid.
        firebaseAuth.currentUser?.toAuthUser()
            ?: firebaseAuth.signInAnonymously().await().user
                ?.toAuthUser()
            ?: error("Anonymous sign-in returned no user")
    }.onFailure { e ->
        // Without this log the failure is invisible — the listener never
        // fires, leaving the UI on a forever-spinner. The most common cause
        // is Anonymous Authentication being disabled in the Firebase Console
        // (Auth → Sign-in method → enable "Anonymous").
        Log.e("AuthRepository", "Anonymous sign-in failed", e)
        telemetry.recordNonFatal(e)
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        // Anonymous-only app: there is no other account to land on, so signing
        // out immediately mints a FRESH anonymous identity. This matches the
        // Account card's promise ("Signing out starts a fresh one"), keeps every
        // backend call authenticated, and — crucially — stops the UI getting
        // stuck on the "resolving identity" spinner (which made sign-out look
        // broken). The new uid gets a brand-new server-side account on its
        // first quota fetch (fresh daily credits, empty history).
        runCatching { firebaseAuth.signInAnonymously().await() }
            .onFailure { e ->
                Log.e("AuthRepository", "Re-sign-in after sign out failed", e)
                telemetry.recordNonFatal(e)
            }
    }
}

private fun FirebaseUser.toAuthUser(): AuthUser =
    AuthUser(uid = uid, isAnonymous = isAnonymous)
