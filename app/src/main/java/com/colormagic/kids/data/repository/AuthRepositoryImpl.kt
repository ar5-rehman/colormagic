package com.colormagic.kids.data.repository

import android.app.Activity
import android.content.Context
import com.colormagic.kids.domain.model.UserProfile
import com.colormagic.kids.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Stubbed implementation. The structure is exactly what the real
// Credential Manager flow will look like — the commented blocks below
// show where to plug in CredentialManager / GoogleIdOption /
// GoogleIdTokenCredential calls once a web client ID is configured.
@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    override suspend fun signInWithGoogle(activity: Activity): Result<UserProfile> =
        runCatching {
            // ─── TODO: real Credential Manager wiring ─────────────────────
            //
            // val credentialManager = CredentialManager.create(context)
            // val googleIdOption = GetGoogleIdOption.Builder()
            //     .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            //     .setFilterByAuthorizedAccounts(false)
            //     .build()
            // val request = GetCredentialRequest.Builder()
            //     .addCredentialOption(googleIdOption)
            //     .build()
            // val result = credentialManager.getCredential(activity, request)
            // val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            // val profile = UserProfile(
            //     id = credential.id,
            //     displayName = credential.displayName.orEmpty(),
            //     email = credential.id,
            //     photoUrl = credential.profilePictureUri?.toString()
            // )
            //
            // ──────────────────────────────────────────────────────────────
            //
            // Stub: simulate latency + return a demo profile.
            delay(600)
            val profile = UserProfile(
                id = "demo-parent-uid",
                displayName = "Demo Parent",
                email = "parent@example.com",
                photoUrl = null
            )
            _currentUser.value = profile
            profile
        }

    override suspend fun signOut() {
        // TODO: credentialManager.clearCredentialState(ClearCredentialStateRequest())
        _currentUser.value = null
    }
}
