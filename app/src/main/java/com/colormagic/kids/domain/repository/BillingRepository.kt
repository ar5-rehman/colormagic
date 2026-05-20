package com.colormagic.kids.domain.repository

import android.app.Activity
import com.colormagic.kids.domain.model.PurchaseResult

// Google Play Billing entry point. The implementation talks to the Play
// Billing SDK and hands every completed purchase to the backend
// `verifyPurchase` function — the client never grants entitlements itself.
interface BillingRepository {

    /**
     * Connects to Google Play and loads product details. Safe to call more
     * than once; best-effort (failures surface later from [purchase]).
     */
    suspend fun start()

    /**
     * Launches the Play purchase dialog for [productId] and suspends until the
     * whole flow finishes — including server-side verification.
     */
    suspend fun purchase(activity: Activity, productId: String): PurchaseResult
}
