package com.colormagic.kids.domain.repository

import android.app.Activity
import com.colormagic.kids.domain.model.PurchaseResult
import kotlinx.coroutines.flow.StateFlow

// Google Play Billing entry point. The implementation talks to the Play
// Billing SDK and hands every completed purchase to the backend
// `verifyPurchase` function — the client never grants entitlements itself.
interface BillingRepository {

    /**
     * Localized display prices from Google Play, keyed by product id
     * (e.g. "$7.99", "Rs 2,200.00"). Empty until product details load — the UI
     * shows its USD fallback until then.
     */
    val prices: StateFlow<Map<String, String>>

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

    /**
     * Queries Google Play for any existing purchases and re-verifies each one
     * with the backend. Used for the "Restore Purchases" button.
     * Returns [PurchaseResult.Success] if at least one active purchase was
     * found and restored, or [PurchaseResult.Failed] otherwise.
     */
    suspend fun restorePurchases(): PurchaseResult

    /**
     * Settles any purchase that was paid but never finished — the app was
     * killed mid-verification, verification failed, or a pending (e.g. cash)
     * payment cleared while the app was closed. Play auto-refunds purchases
     * that aren't acknowledged within 3 days, so this runs on every app start.
     * Returns true if anything was newly credited (caller should refresh quota).
     */
    suspend fun reconcilePurchases(): Boolean
}
