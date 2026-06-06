/**
 * Callable: verifyPurchase
 *
 * Server-side validation of a Google Play purchase. The Android client must
 * NEVER grant entitlements itself — a tampered client could fake "pro". The
 * client only sends the productId + purchaseToken; this function asks Google
 * Play whether the purchase is real, then updates Firestore.
 *
 * Idempotent: each purchaseToken is recorded in `processedPurchases`, so a
 * retried call never double-credits an extra pack.
 *
 * Setup required (one-time):
 *  1. Enable the "Google Play Android Developer API" in Google Cloud.
 *  2. In Play Console → Users & permissions, invite this function's runtime
 *     service account and grant it financial/app access. The GoogleAuth
 *     below then authenticates via Application Default Credentials.
 */
import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {
  ANDROID_PACKAGE_NAME,
  ENFORCE_APP_CHECK,
  EXTRA_PACK_CREDITS,
  MONTHLY_PERIOD_DAYS,
  PRODUCT_EXTRA_20,
  PRODUCT_MONTHLY_PRO,
  PRO_MONTHLY_LIMIT,
  REGION,
} from "./config";
import {computeQuota, ensureUserDoc} from "./credits";
import {Collections, db} from "./firebase";
import {androidPublisher} from "./playApi";
import {VerifyPurchaseRequest, VerifyPurchaseResponse} from "./types";

export const verifyPurchase = onCall(
  {region: REGION, enforceAppCheck: ENFORCE_APP_CHECK},
  async (request): Promise<VerifyPurchaseResponse> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign-in required.");
    }

    const {productId, purchaseToken} =
      (request.data ?? {}) as VerifyPurchaseRequest;
    if (!productId || !purchaseToken) {
      throw new HttpsError(
        "invalid-argument",
        "productId and purchaseToken are required."
      );
    }

    // Idempotency guard (fast path) — already processed → just echo state.
    // NOTE: this is only an optimisation. The AUTHORITATIVE guard runs inside
    // the transactions below via tx.get(ledgerRef), which closes the race where
    // two concurrent calls with the same token both pass this outer check and
    // both grant credits (a double-spend on the consumable extra pack).
    const ledgerRef = db
      .collection(Collections.processedPurchases)
      .doc(purchaseToken);
    if ((await ledgerRef.get()).exists) {
      const existing = await ensureUserDoc(uid);
      return {
        success: true,
        plan: existing.plan,
        totalAvailableCredits: computeQuota(existing).totalAvailableCredits,
      };
    }

    await ensureUserDoc(uid);
    const publisher = androidPublisher();
    const userRef = db.collection(Collections.users).doc(uid);

    if (productId === PRODUCT_MONTHLY_PRO) {
      // ── Subscription ──────────────────────────────────────────────
      const res = await publisher.purchases.subscriptionsv2.get({
        packageName: ANDROID_PACKAGE_NAME,
        token: purchaseToken,
      });
      const state = res.data.subscriptionState;
      const active =
        state === "SUBSCRIPTION_STATE_ACTIVE" ||
        state === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD";
      if (!active) {
        throw new HttpsError(
          "failed-precondition",
          "This subscription is not active."
        );
      }

      // The current cycle's end → when usedSketchesThisMonth resets.
      const expiryIso = res.data.lineItems?.[0]?.expiryTime;
      const resetAt = expiryIso
        ? Timestamp.fromDate(new Date(expiryIso))
        : Timestamp.fromMillis(
            Date.now() + MONTHLY_PERIOD_DAYS * 24 * 60 * 60 * 1000
          );

      await db.runTransaction(async (tx) => {
        // Authoritative idempotency guard — abort if another concurrent call
        // already recorded this token. (All reads must precede writes.)
        if ((await tx.get(ledgerRef)).exists) return;
        tx.update(userRef, {
          plan: "pro",
          subscriptionActive: true,
          monthlySketchLimit: PRO_MONTHLY_LIMIT,
          // Fresh allowance for this billing cycle.
          usedSketchesThisMonth: 0,
          monthlyResetAt: resetAt,
          // Authoritative end of the paid period + token for re-validation.
          subscriptionExpiresAt: resetAt,
          subscriptionPurchaseToken: purchaseToken,
          updatedAt: Timestamp.now(),
        });
        tx.set(ledgerRef, {
          uid,
          productId,
          createdAt: Timestamp.now(),
        });
      });
    } else if (productId === PRODUCT_EXTRA_20) {
      // ── One-time extra pack ───────────────────────────────────────
      const res = await publisher.purchases.products.get({
        packageName: ANDROID_PACKAGE_NAME,
        productId,
        token: purchaseToken,
      });
      // purchaseState: 0 = purchased, 1 = canceled, 2 = pending.
      if (res.data.purchaseState !== 0) {
        throw new HttpsError(
          "failed-precondition",
          "This purchase is not complete."
        );
      }

      await db.runTransaction(async (tx) => {
        // Authoritative idempotency guard — prevents the concurrent
        // double-increment that would mint free credits on the extra pack.
        if ((await tx.get(ledgerRef)).exists) return;
        tx.update(userRef, {
          extraCredits: FieldValue.increment(EXTRA_PACK_CREDITS),
          updatedAt: Timestamp.now(),
        });
        tx.set(ledgerRef, {
          uid,
          productId,
          createdAt: Timestamp.now(),
        });
      });
    } else {
      throw new HttpsError(
        "invalid-argument",
        `Unknown productId: ${productId}`
      );
    }

    const updated = await ensureUserDoc(uid);
    return {
      success: true,
      plan: updated.plan,
      totalAvailableCredits: computeQuota(updated).totalAvailableCredits,
    };
  }
);
