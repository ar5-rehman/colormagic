/**
 * Callable: deleteAccount
 *
 * In-app "Delete account" (Google Play account-deletion requirement + the
 * Privacy Policy's deletion promise). Deletes everything tied to the caller:
 *   • sketches/{id} docs where userId == uid, and Storage sketches/{uid}/…
 *   • feedback/support messages the user sent
 *   • users/{uid} (credits, plan, streak, parent controls)
 *   • the Firebase Auth user itself (onUserDeleted then no-ops)
 *
 * Kept on purpose: processedPurchases / processedAdRewards — the purchase and
 * ad-reward ledgers needed for accounting, refunds and fraud checks (disclosed
 * in the Privacy Policy). A Play subscription is NOT cancelled by this; the
 * parent cancels it in the Play Store.
 */
import {getAuth} from "firebase-admin/auth";
import type {Query} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {DEFAULT_MAX_INSTANCES, ENFORCE_APP_CHECK, REGION} from "./config";
import {Collections, db, storage} from "./firebase";

/** Deletes every doc matched by [query], in batches of 400. */
async function deleteAll(query: Query): Promise<number> {
  let deleted = 0;
  for (;;) {
    const snap = await query.limit(400).get();
    if (snap.empty) return deleted;
    const batch = db.batch();
    snap.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
    deleted += snap.size;
  }
}

export const deleteAccount = onCall(
  {region: REGION, maxInstances: DEFAULT_MAX_INSTANCES, enforceAppCheck: ENFORCE_APP_CHECK},
  async (request): Promise<{success: true}> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign-in required.");
    }

    try {
      await deleteAll(db.collection(Collections.sketches).where("userId", "==", uid));
      await deleteAll(db.collection(Collections.feedback).where("uid", "==", uid));
      await storage.bucket().deleteFiles({prefix: `sketches/${uid}/`});
      await db.collection(Collections.users).doc(uid).delete();
      // Last, so a failure above can be retried by the same signed-in user.
      await getAuth().deleteUser(uid);
    } catch (err) {
      console.error(`deleteAccount failed for ${uid}`, err);
      throw new HttpsError("internal", "We couldn't delete your account. Please try again.");
    }
    return {success: true};
  }
);
