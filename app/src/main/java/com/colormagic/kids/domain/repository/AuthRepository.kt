package com.colormagic.kids.domain.repository

import com.colormagic.kids.domain.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

// The single contract for the app's Firebase identity.
//
// ColorMagic Kids uses *anonymous* auth: the very first launch silently
// creates an anonymous account so every device has a stable `uid` to hang
// credits, sketches and subscription state on — with zero sign-in friction
// for a 4-year-old. The presentation layer only ever talks to this
// interface, never to FirebaseAuth directly, which keeps it swappable
// (e.g. linking to a real credential later) and testable.
interface AuthRepository {

    /**
     * Live auth identity. `null` before the first anonymous sign-in
     * resolves; non-null and stable thereafter for the life of the install.
     */
    val authState: StateFlow<AuthUser?>

    /** Snapshot of the current identity, or null if not yet signed in. */
    val currentUser: AuthUser?

    /**
     * Guarantees an anonymous account exists. Idempotent — if already
     * signed in it just returns the existing user, so it's safe to call on
     * every app launch.
     */
    suspend fun ensureSignedIn(): Result<AuthUser>

    /**
     * Signs the current user out.
     *
     * NOTE: anonymous accounts cannot be recovered. After sign-out the next
     * [ensureSignedIn] mints a brand-new uid — any credits/sketches tied to
     * the old uid become unreachable. Surface this clearly in any UI that
     * calls it (treat it as "reset", not "switch account").
     */
    suspend fun signOut()
}
