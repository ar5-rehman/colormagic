package com.colormagic.kids.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.colormagic.kids.data.di.ApplicationScope
import com.colormagic.kids.data.telemetry.AppTelemetry
import com.colormagic.kids.domain.model.BillingProducts
import com.colormagic.kids.domain.model.PurchaseResult
import com.colormagic.kids.domain.repository.BillingRepository
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

// Wraps the Google Play Billing SDK. Every completed purchase is sent to the
// backend `verifyPurchase` callable, which validates it with Google Play and
// credits Firestore — a tampered client cannot mint "pro" on its own.
@Singleton
class BillingRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val functions: FirebaseFunctions,
    private val telemetry: AppTelemetry,
    @ApplicationScope private val appScope: CoroutineScope
) : BillingRepository, PurchasesUpdatedListener {

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        // Billing 8: the client reconnects to Play on its own after a drop.
        .enableAutoServiceReconnection()
        .build()

    // Product details, loaded once connected. Required to launch a purchase —
    // the subscription flow also needs an offer token taken from these.
    private val productDetails = mutableMapOf<String, ProductDetails>()

    // Only one purchase runs at a time. The Play dialog result arrives
    // asynchronously on onPurchasesUpdated; this bridges it to the caller.
    private val purchaseMutex = Mutex()
    @Volatile private var pendingPurchase: CompletableDeferred<PurchaseResult>? = null

    private val _prices = MutableStateFlow<Map<String, String>>(emptyMap())
    override val prices: StateFlow<Map<String, String>> = _prices.asStateFlow()

    // Purchase tokens currently being verified/settled — stops the purchase
    // flow and a concurrent reconcile from settling the same token twice.
    private val tokensInFlight = mutableSetOf<String>()
    @Volatile private var lastReconcileAt = 0L

    override suspend fun start() {
        if (connect()) loadProductDetails()
    }

    override suspend fun purchase(activity: Activity, productId: String): PurchaseResult =
        purchaseMutex.withLock {
            if (!connect()) return PurchaseResult.Failed(CONNECT_ERROR)
            if (productDetails[productId] == null) loadProductDetails()
            val details = productDetails[productId]
                ?: return PurchaseResult.Failed(PRODUCT_MISSING_ERROR)

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .apply {
                    // Subscriptions require the offer token of the chosen offer.
                    details.subscriptionOfferDetails?.firstOrNull()?.let {
                        setOfferToken(it.offerToken)
                    }
                }
                .build()
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()

            val deferred = CompletableDeferred<PurchaseResult>()
            pendingPurchase = deferred
            val launch = billingClient.launchBillingFlow(activity, flowParams)
            if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
                pendingPurchase = null
                return PurchaseResult.Failed(PURCHASE_ERROR)
            }
            try {
                deferred.await()
            } finally {
                pendingPurchase = null
            }
        }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        val deferred = pendingPurchase
        if (deferred == null) {
            // No purchase dialog is open: a pending payment just cleared (or
            // Play re-delivered a purchase). Settle it in the background so it
            // isn't auto-refunded.
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.orEmpty().forEach { appScope.launch { handlePurchase(it) } }
            }
            return
        }
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull()
                if (purchase == null) {
                    deferred.complete(PurchaseResult.Failed(PURCHASE_ERROR))
                } else {
                    // Verification + settlement must run off the listener thread.
                    appScope.launch { deferred.complete(handlePurchase(purchase)) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                deferred.complete(PurchaseResult.Cancelled)
            else ->
                deferred.complete(PurchaseResult.Failed(PURCHASE_ERROR))
        }
    }

    private suspend fun handlePurchase(purchase: Purchase): PurchaseResult {
        val token = purchase.purchaseToken
        synchronized(tokensInFlight) {
            if (!tokensInFlight.add(token)) return PurchaseResult.Pending
        }
        return try {
            settlePurchase(purchase)
        } finally {
            synchronized(tokensInFlight) { tokensInFlight.remove(token) }
        }
    }

    private suspend fun settlePurchase(purchase: Purchase): PurchaseResult {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> return PurchaseResult.Pending
            Purchase.PurchaseState.PURCHASED -> Unit
            else -> return PurchaseResult.Failed(PURCHASE_ERROR)
        }
        val productId = purchase.products.firstOrNull()
            ?: return PurchaseResult.Failed(PURCHASE_ERROR)

        // Server validates with Google Play and credits Firestore.
        val verified = verifyOnBackend(productId, purchase.purchaseToken)
            ?: return PurchaseResult.Failed(VERIFY_ERROR)

        // Settle the purchase with Play so it isn't auto-refunded after 3 days.
        //   Consumable extra pack → consume so it can be bought again.
        //   Subscription          → acknowledge once.
        if (productId == BillingProducts.EXTRA_20) {
            consume(purchase.purchaseToken)
        } else if (!purchase.isAcknowledged) {
            acknowledge(purchase.purchaseToken)
        }
        return verified
    }

    private suspend fun verifyOnBackend(
        productId: String,
        purchaseToken: String
    ): PurchaseResult.Success? = runCatching {
        val response = functions
            .getHttpsCallable(FN_VERIFY_PURCHASE)
            .call(mapOf("productId" to productId, "purchaseToken" to purchaseToken))
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = response.getData() as Map<String, Any?>
        PurchaseResult.Success(
            plan = data["plan"] as? String ?: "free",
            totalAvailableCredits = (data["totalAvailableCredits"] as? Number)?.toInt() ?: 0
        )
    }.onFailure { telemetry.recordNonFatal(it) }.getOrNull()

    private suspend fun connect(): Boolean {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        // BILLING_UNAVAILABLE (3) → device account / Play Store
                        // can't transact (no Play Store, not signed in, sandbox
                        // not configured, app not uploaded to a Play track).
                        Log.e(
                            TAG,
                            "Billing connect failed: code=${result.responseCode} " +
                                "msg=${result.debugMessage}"
                        )
                    }
                    if (cont.isActive) {
                        cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "Billing service disconnected")
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }

    private suspend fun loadProductDetails() {
        // Billing requires every product in a single query to be the same
        // type — subscription and one-time products are fetched separately.
        queryByType(BillingProducts.MONTHLY_PRO, BillingClient.ProductType.SUBS)
        queryByType(BillingProducts.EXTRA_20, BillingClient.ProductType.INAPP)
    }

    private suspend fun queryByType(productId: String, productType: String) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(productType)
                        .build()
                )
            )
            .build()

        val details = suspendCancellableCoroutine<List<ProductDetails>> { cont ->
            // Billing 8: the callback returns a QueryProductDetailsResult —
            // fetched products plus any Play couldn't return.
            billingClient.queryProductDetailsAsync(params) { result, queryResult ->
                val list = queryResult.productDetailsList
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(
                        TAG,
                        "queryProductDetails $productId/$productType failed: " +
                            "code=${result.responseCode} msg=${result.debugMessage}"
                    )
                } else if (list.isEmpty()) {
                    // Connection OK but Play returned no product. Usually means
                    // the productId isn't configured/active in the Play Console
                    // for this app's signing key, or the test account doesn't
                    // have access to the track this build is uploaded to.
                    Log.w(TAG, "Product '$productId' ($productType) not found in Play Console")
                }
                if (cont.isActive) {
                    cont.resume(
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) list
                        else emptyList()
                    )
                }
            }
        }
        details.forEach { productDetails[it.productId] = it }
        val formatted = details.mapNotNull { d ->
            val price = d.subscriptionOfferDetails
                ?.firstOrNull()
                // The LAST pricing phase is the recurring price (earlier
                // phases would be a free trial / intro offer).
                ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                ?: d.oneTimePurchaseOfferDetails?.formattedPrice
            price?.let { d.productId to it }
        }
        if (formatted.isNotEmpty()) _prices.update { it + formatted }
    }

    override suspend fun reconcilePurchases(): Boolean {
        // Runs on every Home resume — at most once a minute is plenty.
        val now = System.currentTimeMillis()
        if (now - lastReconcileAt < RECONCILE_INTERVAL_MS) return false
        lastReconcileAt = now
        if (!connect()) return false

        val owned = queryOwned(BillingClient.ProductType.INAPP) +
            queryOwned(BillingClient.ProductType.SUBS)
        var credited = false
        owned.filter { p ->
            p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                // An owned one-time pack is by definition unconsumed (= not
                // settled); a subscription only needs work until acknowledged.
                (p.products.contains(BillingProducts.EXTRA_20) || !p.isAcknowledged)
        }.forEach { purchase ->
            if (handlePurchase(purchase) is PurchaseResult.Success) credited = true
        }
        return credited
    }

    private suspend fun queryOwned(productType: String): List<Purchase> =
        suspendCancellableCoroutine { cont ->
            val params = QueryPurchasesParams.newBuilder().setProductType(productType).build()
            billingClient.queryPurchasesAsync(params) { result, list ->
                if (cont.isActive) {
                    cont.resume(
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) list
                        else emptyList()
                    )
                }
            }
        }

    private suspend fun consume(purchaseToken: String) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        suspendCancellableCoroutine<Unit> { cont ->
            billingClient.consumeAsync(params) { _, _ -> if (cont.isActive) cont.resume(Unit) }
        }
    }

    override suspend fun restorePurchases(): PurchaseResult {
        if (!connect()) return PurchaseResult.Failed(CONNECT_ERROR)

        val purchases = queryOwned(BillingClient.ProductType.SUBS)

        val activePurchase = purchases.firstOrNull {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        } ?: return PurchaseResult.Failed("No active purchases found.")

        val productId = activePurchase.products.firstOrNull()
            ?: return PurchaseResult.Failed(PURCHASE_ERROR)
        return verifyOnBackend(productId, activePurchase.purchaseToken)
            ?: PurchaseResult.Failed(VERIFY_ERROR)
    }

    private suspend fun acknowledge(purchaseToken: String) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        suspendCancellableCoroutine<Unit> { cont ->
            billingClient.acknowledgePurchase(params) { if (cont.isActive) cont.resume(Unit) }
        }
    }

    private companion object {
        const val TAG = "BillingRepository"
        const val FN_VERIFY_PURCHASE = "verifyPurchase"
        const val CONNECT_ERROR = "Couldn't reach Google Play. Please try again."
        const val PRODUCT_MISSING_ERROR =
            "This product isn't available yet. Please try again later."
        const val PURCHASE_ERROR = "Something went wrong with the purchase. Please try again."
        const val VERIFY_ERROR = "We couldn't confirm your purchase. Please try again."
        const val RECONCILE_INTERVAL_MS = 60_000L
    }
}
