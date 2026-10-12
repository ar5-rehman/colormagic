package com.colormagic.kids.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.colormagic.kids.data.di.ApplicationScope
import com.colormagic.kids.data.telemetry.AppTelemetry
import com.colormagic.kids.domain.model.AuthUser
import com.colormagic.kids.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

// Max time to wait for a Firebase sign-in before giving up so the UI never
// hangs on an endless spinner.
private const val SIGN_IN_TIMEOUT_MS = 15_000L

// Which uid this phone was last registered to (skip redundant registerDevice calls).
private const val KEY_REGISTERED_UID = "registered_uid"

// Firebase-backed implementation of [AuthRepository].
//
// Anonymous auth only. The OpenAI key, credit ledger and purchase
// verification all live on the Firebase backend; this class just makes sure
// the device has a verified Firebase identity to attach those calls to.
@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val functions: FirebaseFunctions,
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
        // Already signed in (returning user) → reuse the existing uid, and make
        // sure this phone points at it (covers installs from before this
        // feature; a no-op network-wise once registered).
        firebaseAuth.currentUser?.toAuthUser()
            ?.also { user -> appScope.launch { registerIfNeeded(user.uid) } }
        // Otherwise (fresh install / reinstall) — with a HARD TIMEOUT so a hung
        // network/Firebase call can never leave the UI spinning forever:
        //  1. if this phone already has an account, sign back into it;
        //  2. else create a guest account and remember it for this phone.
            ?: withTimeoutOrNull(SIGN_IN_TIMEOUT_MS) {
                restoreDeviceAccount()
                    ?: firebaseAuth.signInAnonymously().await().user?.toAuthUser()
                        ?.also { registerThisDevice() }
            }
            ?: error("Couldn't reach the server. Check your internet and tap Try again.")
    }.onFailure { e ->
        Log.e("AuthRepository", "Anonymous sign-in failed", e)
        telemetry.recordNonFatal(e)
    }

    /**
     * A one-way hash of the Android ID. ANDROID_ID survives uninstall/reinstall
     * and is unique per app-signing key (other apps see a different value); it
     * only changes on factory reset. Hashing means the raw ID never leaves the
     * phone.
     */
    @SuppressLint("HardwareIds")
    private fun deviceKey(): String? {
        val androidId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        )?.takeIf { it.isNotBlank() } ?: return null
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("colormagic-device-v1:$androidId".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** Signs back into the account last used on this phone, if there is one. */
    private suspend fun restoreDeviceAccount(): AuthUser? = runCatching {
        val key = deviceKey() ?: return null
        val response = functions.getHttpsCallable("restoreDeviceAccount")
            .call(mapOf("deviceKey" to key)).await()
        @Suppress("UNCHECKED_CAST")
        val token = (response.getData() as? Map<String, Any?>)?.get("token") as? String
            ?: return null
        firebaseAuth.signInWithCustomToken(token).await().user?.toAuthUser()
    }.onFailure {
        // Best-effort: on any failure we just start a new guest account.
        Log.w("AuthRepository", "Device account restore failed", it)
        telemetry.recordNonFatal(it)
    }.getOrNull()

    /** Remembers "this phone → current account" so a reinstall restores it. */
    private suspend fun registerThisDevice() {
        val key = deviceKey() ?: return
        val uid = firebaseAuth.currentUser?.uid ?: return
        runCatching {
            functions.getHttpsCallable("registerDevice")
                .call(mapOf("deviceKey" to key)).await()
            devicePrefs.edit().putString(KEY_REGISTERED_UID, uid).apply()
        }.onFailure { Log.w("AuthRepository", "registerDevice failed", it) }
    }

    private suspend fun registerIfNeeded(uid: String) {
        if (devicePrefs.getString(KEY_REGISTERED_UID, null) != uid) registerThisDevice()
    }

    private val devicePrefs by lazy {
        context.getSharedPreferences("device_account", Context.MODE_PRIVATE)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = firebaseAuth.currentUser

        val user = withTimeoutOrNull(SIGN_IN_TIMEOUT_MS) {
            // "Guest" = no Google login yet. (A guest restored after a reinstall
            // signs in with a custom token, so Firebase's isAnonymous is false.)
            val result = if (current != null && current.isGuest()) {
                // Upgrade the guest in place — same uid, so credits/art carry over.
                try {
                    // New Google account → attached to this same account, so
                    // every credit and purchase simply stays.
                    current.linkWithCredential(credential).await().also {
                        signInSummary = "You're signed in. Your credits are now saved " +
                            "to your Google account."
                    }
                } catch (collision: FirebaseAuthUserCollisionException) {
                    // This Google account already has its own account → switch to
                    // it and MOVE everything the guest had into it. The guest's ID
                    // token (taken before switching) proves we own the guest.
                    val guestToken = current.getIdToken(false).await().token
                    val switched = firebaseAuth.signInWithCredential(credential).await()
                    signInSummary = guestToken?.let { mergeGuestInto(it) }
                    switched
                }
            } else {
                firebaseAuth.signInWithCredential(credential).await()
            }
            result.user?.toAuthUser()
        }
        registerThisDevice()
        // Signing back into a deleted account (within 60 days) restores it.
        runCatching { functions.getHttpsCallable("reactivateAccount").call().await() }
        user ?: error("Google sign-in timed out. Please try again.")
    }.onFailure { e ->
        Log.e("AuthRepository", "Google sign-in failed", e)
        telemetry.recordNonFatal(e)
    }

    /** One-shot message describing what happened on the last Google sign-in. */
    @Volatile private var signInSummary: String? = null

    override fun consumeSignInSummary(): String? = signInSummary.also { signInSummary = null }

    /** Moves the guest's credits/Premium into the signed-in Google account. */
    private suspend fun mergeGuestInto(guestIdToken: String): String? = runCatching {
        val response = functions.getHttpsCallable("mergeGuestAccount")
            .call(mapOf("guestIdToken" to guestIdToken)).await()
        @Suppress("UNCHECKED_CAST")
        val data = response.getData() as Map<String, Any?>
        val credits = (data["movedCredits"] as? Number)?.toInt() ?: 0
        val premium = data["movedPremium"] as? Boolean ?: false
        when {
            premium && credits > 0 ->
                "Premium and $credits credits from this phone were moved to your Google account."
            premium -> "Premium from this phone was moved to your Google account."
            credits > 0 ->
                "$credits credit${if (credits == 1) "" else "s"} from this phone " +
                    "${if (credits == 1) "was" else "were"} moved to your Google account."
            else -> null
        }
    }.onFailure {
        Log.e("AuthRepository", "mergeGuestAccount failed", it)
        telemetry.recordNonFatal(it)
    }.getOrNull()

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        functions.getHttpsCallable("deleteAccount")
            .call(mapOf("deviceKey" to deviceKey())).await()
        // The old account is now inactive (restorable for 60 days). Continue
        // on a NEW empty account; the server limits its free credits per phone.
        startFreshGuest()
    }.onFailure { e ->
        Log.e("AuthRepository", "Account deletion failed", e)
        telemetry.recordNonFatal(e)
    }

    override suspend fun restoreDeletedAccount(): Result<Unit> = runCatching {
        val key = deviceKey() ?: error("This phone can't be identified.")
        val response = functions.getHttpsCallable("restoreDeletedAccount")
            .call(mapOf("deviceKey" to key)).await()
        @Suppress("UNCHECKED_CAST")
        val token = (response.getData() as Map<String, Any?>)["token"] as String
        firebaseAuth.signOut()
        val uid = firebaseAuth.signInWithCustomToken(token).await().user?.uid
        // The server already points this phone at the restored account.
        uid?.let { devicePrefs.edit().putString(KEY_REGISTERED_UID, it).apply() }
        Unit
    }.onFailure { e ->
        Log.e("AuthRepository", "Account restore failed", e)
        telemetry.recordNonFatal(e)
    }

    /** Signs out and continues on a brand-new guest account for this phone. */
    private suspend fun startFreshGuest() {
        firebaseAuth.signOut()
        runCatching {
            withTimeoutOrNull(SIGN_IN_TIMEOUT_MS) {
                firebaseAuth.signInAnonymously().await()
            } ?: error("Re-sign-in timed out")
            registerThisDevice()
        }.onFailure { e ->
            Log.e("AuthRepository", "New guest sign-in failed", e)
            telemetry.recordNonFatal(e)
            // AuthViewModel's recovery collector retries and shows an error
            // if this keeps failing — never an endless spinner.
        }
    }

    override suspend fun signOut() {
        val user = firebaseAuth.currentUser ?: return
        if (user.isGuest()) return
        // Everything (credits, streak, purchases, settings) stays with the
        // Google account — signing in again brings it all back. The phone
        // continues on a separate guest account; the server gives it no free
        // credits because it isn't this phone's first account.
        startFreshGuest()
    }
}

/** No Google login yet — a plain guest, or a guest restored via custom token. */
private fun FirebaseUser.isGuest(): Boolean =
    providerData.none { it.providerId == GoogleAuthProvider.PROVIDER_ID }

private fun FirebaseUser.toAuthUser(): AuthUser {
    // When a guest LINKS a Google account, Firebase doesn't always copy the
    // Google name/email/photo onto the top-level user profile — they live in
    // providerData instead. Fall back to that so the UI always has them.
    val google = providerData.firstOrNull { it.providerId == "google.com" }
    return AuthUser(
        uid = uid,
        isAnonymous = isGuest(),
        email = email?.takeIf { it.isNotBlank() } ?: google?.email,
        displayName = displayName?.takeIf { it.isNotBlank() } ?: google?.displayName,
        photoUrl = (photoUrl ?: google?.photoUrl)?.toString()
    )
}
