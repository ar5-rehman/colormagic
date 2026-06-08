package com.colormagic.kids.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialInterruptedException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the Google account picker (Credential Manager) and returns a Google
 * ID token, which [AuthRepository.signInWithGoogle] exchanges for a Firebase
 * credential.
 *
 * Requires the OAuth **web client ID**, which the google-services plugin emits
 * as the string resource `default_web_client_id` — but ONLY after Google
 * sign-in is enabled in the Firebase console and google-services.json is
 * re-downloaded. We resolve it by name at runtime so the app still compiles
 * before that's configured (the call just fails with a clear message).
 */
@Singleton
class GoogleSignInHelper @Inject constructor() {

    /**
     * Shows the Google account chooser and returns the selected account's ID
     * token. Must be called with an Activity [context] (it presents UI).
     */
    suspend fun getGoogleIdToken(context: Context): Result<String> = runCatching {
        val resId = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        )
        require(resId != 0) {
            "Google sign-in isn't configured yet. Enable Google in Firebase " +
                "Authentication and re-download google-services.json."
        }
        val webClientId = context.getString(resId)

        val googleIdOption = GetGoogleIdOption.Builder()
            // false → let the user pick ANY Google account, even the first time.
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)
        // GetCredentialInterruptedException is a transient error (the system UI
        // was interrupted, or a race with Play services). Retrying once clears
        // most of the intermittent "sign-in failed" cases.
        val response = try {
            credentialManager.getCredential(context, request)
        } catch (e: GetCredentialInterruptedException) {
            credentialManager.getCredential(context, request)
        }
        val credential = response.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } else {
            error("Unexpected credential type: ${credential.type}")
        }
    }
}
