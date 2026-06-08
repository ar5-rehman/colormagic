package com.colormagic.kids.data.repository

import android.util.Log
import com.colormagic.kids.data.di.ApplicationScope
import com.colormagic.kids.data.telemetry.AppTelemetry
import com.colormagic.kids.domain.model.AuthUser
import com.colormagic.kids.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

// Max time to wait for a Firebase sign-in before giving up so the UI never
// hangs on an endless spinner.
private const val SIGN_IN_TIMEOUT_MS = 15_000L

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
            // IdTokenListener (not just AuthStateListener): it also fires when
            // the SAME signed-in user changes — e.g. when a guest LINKS a Google
            // account. linkWithCredential refreshes the ID token but keeps the
            // same uid, so AuthStateListener wouldn't fire and the UI would keep
            // showing "Sign in with Google" even though the user is now Google.
            val listener = FirebaseAuth.IdTokenListener { auth ->
                val user = auth.currentUser?.toAuthUser()
                telemetry.setUserId(user?.uid)
                trySend(user)
            }
            firebaseAuth.addIdTokenListener(listener)
            awaitClose { firebaseAuth.removeIdTokenListener(listener) }
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
        // Otherwise sign in anonymously, but with a HARD TIMEOUT so a hung
        // network/Firebase call can never leave the UI spinning forever.
        // withTimeoutOrNull returns null on timeout → we convert that into a
        // clear, retryable error instead of an infinite wait.
            ?: withTimeoutOrNull(SIGN_IN_TIMEOUT_MS) {
                firebaseAuth.signInAnonymously().await().user?.toAuthUser()
            }
            ?: error("Couldn't reach the server. Check your internet and tap Try again.")
    }.onFailure { e ->
        Log.e("AuthRepository", "Anonymous sign-in failed", e)
        telemetry.recordNonFatal(e)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = firebaseAuth.currentUser

        val user = withTimeoutOrNull(SIGN_IN_TIMEOUT_MS) {
            val result = if (current != null && current.isAnonymous) {
                // Upgrade the guest in place — same uid, so credits/art carry over.
                try {
                    current.linkWithCredential(credential).await()
                } catch (collision: FirebaseAuthUserCollisionException) {
                    // This Google account already has its own Firebase user → sign
                    // into that instead. (The anonymous uid is abandoned.)
                    firebaseAuth.signInWithCredential(credential).await()
                }
            } else {
                firebaseAuth.signInWithCredential(credential).await()
            }
            result.user?.toAuthUser()
        }
        user ?: error("Google sign-in timed out. Please try again.")
    }.onFailure { e ->
        Log.e("AuthRepository", "Google sign-in failed", e)
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
        runCatching {
            withTimeoutOrNull(SIGN_IN_TIMEOUT_MS) {
                firebaseAuth.signInAnonymously().await()
            } ?: error("Re-sign-in timed out")
        }.onFailure { e ->
            Log.e("AuthRepository", "Re-sign-in after sign out failed", e)
            telemetry.recordNonFatal(e)
            // The AuthViewModel's recovery collector will retry and surface a
            // visible error if this keeps failing — never an endless spinner.
        }
    }
}

private fun FirebaseUser.toAuthUser(): AuthUser {
    // When a guest LINKS a Google account, Firebase doesn't always copy the
    // Google name/email/photo onto the top-level user profile — they live in
    // providerData instead. Fall back to that so the UI always has them.
    val google = providerData.firstOrNull { it.providerId == "google.com" }
    return AuthUser(
        uid = uid,
        isAnonymous = isAnonymous,
        email = email?.takeIf { it.isNotBlank() } ?: google?.email,
        displayName = displayName?.takeIf { it.isNotBlank() } ?: google?.displayName,
        photoUrl = (photoUrl ?: google?.photoUrl)?.toString()
    )
}
