package com.colormagic.kids.domain.model

// Mirror of the backend `userQuota` response — how many sketches the account
// can still generate. `totalAvailableCredits` is what the UI shows as
// "Sketches left: N".
data class UserQuota(
    val plan: String,
    val subscriptionActive: Boolean,
    val remainingFreeSketches: Int,
    val remainingMonthlySketches: Int,
    val extraCredits: Int,
    val totalAvailableCredits: Int
) {
    companion object {
        /** Safe placeholder before the first network fetch resolves. */
        val UNKNOWN = UserQuota(
            plan = "free",
            subscriptionActive = false,
            remainingFreeSketches = 0,
            remainingMonthlySketches = 0,
            extraCredits = 0,
            totalAvailableCredits = 0
        )
    }
}
