package com.colormagic.kids.domain.model

// Google Play product IDs. These MUST match the products configured in the
// Play Console *and* the IDs the backend `verifyPurchase` function recognises.
object BillingProducts {
    /** Monthly Pro subscription — 50 sketches per billing cycle. */
    const val MONTHLY_PRO = "monthly_pro"

    /** One-time consumable — adds 20 extra sketch credits. */
    const val EXTRA_20 = "extra_20_sketches"
}
