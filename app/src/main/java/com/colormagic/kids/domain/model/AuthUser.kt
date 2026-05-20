package com.colormagic.kids.domain.model

// The signed-in Firebase identity. For ColorMagic Kids this is always an
// anonymous account — created automatically on first launch — so the only
// fields that matter are the uid (the key for Firestore credits/sketches)
// and whether the account is still anonymous (vs. later linked to a real
// credential, should that ever be added).
data class AuthUser(
    val uid: String,
    val isAnonymous: Boolean
)
