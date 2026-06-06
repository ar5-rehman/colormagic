package com.colormagic.kids.domain.model

// The signed-in Firebase identity.
//
// On first launch this is an anonymous (guest) account. The parent can later
// "Sign in with Google", which LINKS the Google credential to the same uid —
// preserving all credits/sketches/subscription state. Once linked, the account
// is no longer anonymous and the email/name/photo are populated.
data class AuthUser(
    val uid: String,
    val isAnonymous: Boolean,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
)
