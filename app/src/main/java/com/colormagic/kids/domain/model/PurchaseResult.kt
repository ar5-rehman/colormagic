package com.colormagic.kids.domain.model

// Outcome of a Play Billing purchase flow, after backend verification.
sealed interface PurchaseResult {
    /** Purchase completed and the backend credited the user's account. */
    data class Success(
        val plan: String,
        val totalAvailableCredits: Int
    ) : PurchaseResult

    /** The parent dismissed the Google Play dialog. */
    data object Cancelled : PurchaseResult

    /** Payment is still processing (e.g. slow card / cash). Credits arrive later. */
    data object Pending : PurchaseResult

    /** The flow or server verification failed; [message] is kid-parent-safe. */
    data class Failed(val message: String) : PurchaseResult
}
