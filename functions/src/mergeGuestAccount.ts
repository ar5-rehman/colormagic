/**
 * Callable: mergeGuestAccount
 *
 * When a guest signs in with a Google account that ALREADY has its own
 * Firebase account, the app switches to that Google account — so everything
 * the guest had on this phone is moved into it here:
 *   • free credits (welcome + rewarded-ad) and purchased extra credits
 *   • Premium, if the guest bought it and the Google account doesn't have it
 *     (plus the purchase ledger, so Play renewals/cancellations follow it)
 *   • the guest's generated sketches
 * The empty guest account is then removed.
 *
 * Proof of ownership: the app sends the guest's Firebase ID token, captured
 * BEFORE switching accounts; we verify it, so nobody can drain another
 * person's guest account by knowing its uid.
 * (A guest signing in with a NEW Google account needs none of this — the
 * Google login is attached to the same account, so nothing moves.)
 */
import {getAuth} from "firebase-admin/auth";
import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {DEFAULT_MAX_INSTANCES, ENFORCE_APP_CHECK, REGION} from "./config";
import {ensureUserDoc} from "./credits";
import {Collections, db} from "./firebase";
import {UserDoc} from "./types";

interface MergeResult {
  movedCredits: number;
  movedPremium: boolean;
}

export const mergeGuestAccount = onCall(
  {region: REGION, maxInstances: DEFAULT_MAX_INSTANCES, enforceAppCheck: ENFORCE_APP_CHECK},
  async (request): Promise<MergeResult> => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign-in required.");
    const guestToken = (request.data ?? {}).guestIdToken;
    if (typeof guestToken !== "string" || !guestToken) {
      throw new HttpsError("invalid-argument", "guestIdToken is required.");
    }

    let guestUid: string;
    try {
      // checkRevoked=false: the guest may be signed out already.
      guestUid = (await getAuth().verifyIdToken(guestToken)).uid;
    } catch {
      throw new HttpsError("permission-denied", "Couldn't verify the guest account.");
    }
    if (guestUid === uid) return {movedCredits: 0, movedPremium: false};

    const guestRef = db.collection(Collections.users).doc(guestUid);
    const userRef = db.collection(Collections.users).doc(uid);
    await ensureUserDoc(uid);

    const result = await db.runTransaction(async (tx): Promise<MergeResult | null> => {
      const guestSnap = await tx.get(guestRef);
      const userSnap = await tx.get(userRef);
      if (!guestSnap.exists) return null; // already merged (retry) / nothing to move
      const guest = guestSnap.data() as UserDoc;
      const user = userSnap.data() as UserDoc;

      const freeCredits = Math.max(0, guest.adCredits ?? 0);
      const extraCredits = Math.max(0, guest.extraCredits ?? 0);
      const guestPro = guest.plan === "pro" && guest.subscriptionActive;
      const userPro = user.plan === "pro" && user.subscriptionActive;
      const movePremium = guestPro && !userPro;

      tx.update(userRef, {
        adCredits: FieldValue.increment(freeCredits),
        extraCredits: FieldValue.increment(extraCredits),
        ...(movePremium ? {
          plan: "pro",
          subscriptionActive: true,
          monthlySketchLimit: guest.monthlySketchLimit,
          usedSketchesThisMonth: guest.usedSketchesThisMonth,
          monthlyResetAt: guest.monthlyResetAt,
          subscriptionExpiresAt: guest.subscriptionExpiresAt,
          subscriptionPurchaseToken: guest.subscriptionPurchaseToken,
        } : {}),
        updatedAt: Timestamp.now(),
      });
      tx.delete(guestRef);
      return {movedCredits: freeCredits + extraCredits, movedPremium: movePremium};
    });
    if (!result) return {movedCredits: 0, movedPremium: false};

    // Purchases + sketches now belong to the Google account.
    const ledger = await db.collection(Collections.processedPurchases)
      .where("uid", "==", guestUid).get();
    const sketches = await db.collection(Collections.sketches)
      .where("userId", "==", guestUid).get();
    const writes = [
      ...ledger.docs.map((d) => d.ref),
      ...sketches.docs.map((d) => d.ref),
    ];
    for (let i = 0; i < writes.length; i += 400) {
      const batch = db.batch();
      writes.slice(i, i + 400).forEach((ref) => {
        batch.update(ref, ref.parent.id === Collections.sketches ? {userId: uid} : {uid});
      });
      await batch.commit();
    }

    await getAuth().deleteUser(guestUid).catch((err) =>
      console.warn(`mergeGuestAccount: couldn't delete guest ${guestUid}`, err));
    console.log(`mergeGuestAccount: ${guestUid} → ${uid}`, result);
    return result;
  }
);
