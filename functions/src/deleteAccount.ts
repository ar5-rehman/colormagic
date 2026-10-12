/**
 * Account deletion with a 60-day restore window (ACCOUNT_RESTORE_DAYS).
 *
 *  • deleteAccount (callable): SOFT delete. The account is marked
 *    deletionRequestedAt / purgeAfter and becomes inactive; the phone
 *    remembers it (devices/{key}.deletedUid) and the app moves to a new empty
 *    account. Free credits stay limited per phone (deviceLimits.ts), so
 *    deleting never hands out extra free credits.
 *  • restoreDeletedAccount (callable): within the window, brings the old
 *    account back on this phone and returns a custom token to sign into it.
 *  • reactivateAccount (callable): signing in again with the deleted account's
 *    Google login also brings it back.
 *  • purgeDeletedAccounts (daily schedule): after the window, PERMANENTLY
 *    erases everything: sketches (docs + Storage files), support messages,
 *    users/{uid}, and the Firebase Auth user.
 *
 * Kept on purpose: processedPurchases / processedAdRewards ledgers
 * (accounting, refunds, fraud) and the phone's credit counters (no personal
 * data — only a hashed device key and dates/counts). A Play subscription is
 * NOT cancelled by deletion; the parent cancels it in the Play Store.
 */
import {getAuth} from "firebase-admin/auth";
import {FieldValue, Timestamp} from "firebase-admin/firestore";
import type {Query} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {onSchedule} from "firebase-functions/v2/scheduler";
import {
  ACCOUNT_RESTORE_DAYS,
  DEFAULT_MAX_INSTANCES,
  ENFORCE_APP_CHECK,
  REGION,
} from "./config";
import {ensureUserDoc} from "./credits";
import {parseDeviceKey} from "./deviceAccount";
import {Collections, db, storage} from "./firebase";

const DAY_MS = 24 * 60 * 60 * 1000;

/** Deletes every doc matched by [query], in batches of 400. */
async function deleteAll(query: Query): Promise<void> {
  for (;;) {
    const snap = await query.limit(400).get();
    if (snap.empty) return;
    const batch = db.batch();
    snap.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
  }
}

/** Removes "deleted account" markers that point at [uid] from phone docs. */
async function clearDeviceDeletionMarkers(uid: string): Promise<void> {
  const snap = await db.collection(Collections.devices).where("deletedUid", "==", uid).get();
  await Promise.all(snap.docs.map((d) => d.ref.update({
    deletedUid: FieldValue.delete(),
    deletedAt: FieldValue.delete(),
    purgeAfter: FieldValue.delete(),
  })));
}

/** PERMANENT erase of an account and its content. */
export async function hardDeleteAccount(uid: string): Promise<void> {
  await deleteAll(db.collection(Collections.sketches).where("userId", "==", uid));
  await deleteAll(db.collection(Collections.feedback).where("uid", "==", uid));
  await storage.bucket().deleteFiles({prefix: `sketches/${uid}/`});
  await clearDeviceDeletionMarkers(uid);
  await db.collection(Collections.users).doc(uid).delete();
  await getAuth().deleteUser(uid).catch((err: {code?: string}) => {
    if (err?.code !== "auth/user-not-found") throw err;
  });
}

/** Cancels a pending deletion of [uid] (it's being used again). */
async function cancelDeletion(uid: string): Promise<boolean> {
  const ref = db.collection(Collections.users).doc(uid);
  const snap = await ref.get();
  if (!snap.exists || !snap.get("purgeAfter")) return false;
  await ref.update({
    deletionRequestedAt: FieldValue.delete(),
    purgeAfter: FieldValue.delete(),
    updatedAt: Timestamp.now(),
  });
  await clearDeviceDeletionMarkers(uid);
  return true;
}

export const deleteAccount = onCall(
  {region: REGION, maxInstances: DEFAULT_MAX_INSTANCES, enforceAppCheck: ENFORCE_APP_CHECK},
  async (request): Promise<{success: true; purgeAfter: number}> => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign-in required.");
    const rawKey = (request.data ?? {}).deviceKey;

    try {
      await ensureUserDoc(uid);
      const now = Timestamp.now();
      const purgeAfter = Timestamp.fromMillis(now.toMillis() + ACCOUNT_RESTORE_DAYS * DAY_MS);
      await db.collection(Collections.users).doc(uid).update({
        deletionRequestedAt: now,
        purgeAfter,
        updatedAt: now,
      });

      // Every phone using this account remembers it, so it can be restored
      // there (and so that phone's free-credit limits keep applying).
      const marker = {deletedUid: uid, deletedAt: now, purgeAfter};
      const phones = await db.collection(Collections.devices).where("uid", "==", uid).get();
      await Promise.all(phones.docs.map((d) => d.ref.set(marker, {merge: true})));
      if (rawKey) {
        await db.collection(Collections.devices).doc(parseDeviceKey(rawKey))
          .set({...marker, uid}, {merge: true});
      }
      return {success: true, purgeAfter: purgeAfter.toMillis()};
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error(`deleteAccount failed for ${uid}`, err);
      throw new HttpsError("internal", "We couldn't delete your account. Please try again.");
    }
  }
);

export const restoreDeletedAccount = onCall(
  {region: REGION, maxInstances: DEFAULT_MAX_INSTANCES, enforceAppCheck: ENFORCE_APP_CHECK},
  async (request): Promise<{token: string}> => {
    const callerUid = request.auth?.uid;
    if (!callerUid) throw new HttpsError("unauthenticated", "Sign-in required.");
    const key = parseDeviceKey((request.data ?? {}).deviceKey);

    const deviceRef = db.collection(Collections.devices).doc(key);
    const deletedUid = (await deviceRef.get()).get("deletedUid") as string | undefined;
    if (!deletedUid) {
      throw new HttpsError("not-found", "There's no deleted account to restore on this phone.");
    }
    const old = await db.collection(Collections.users).doc(deletedUid).get();
    const purgeAfter = old.get("purgeAfter") as Timestamp | undefined;
    if (!old.exists || !purgeAfter || purgeAfter.toMillis() <= Date.now()) {
      throw new HttpsError("failed-precondition", "That account can no longer be restored.");
    }

    await cancelDeletion(deletedUid);
    await deviceRef.set({uid: deletedUid, updatedAt: Timestamp.now()}, {merge: true});

    // The temporary empty account made after deletion is no longer needed —
    // erase it unless something was bought on it.
    if (callerUid !== deletedUid) {
      const temp = await db.collection(Collections.users).doc(callerUid).get();
      const bought = temp.exists &&
        ((temp.get("extraCredits") ?? 0) > 0 || temp.get("plan") === "pro");
      if (!bought) {
        await hardDeleteAccount(callerUid).catch((err) =>
          console.warn(`Couldn't remove temporary account ${callerUid}`, err));
      }
    }
    return {token: await getAuth().createCustomToken(deletedUid)};
  }
);

export const reactivateAccount = onCall(
  {region: REGION, maxInstances: DEFAULT_MAX_INSTANCES, enforceAppCheck: ENFORCE_APP_CHECK},
  async (request): Promise<{reactivated: boolean}> => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign-in required.");
    return {reactivated: await cancelDeletion(uid)};
  }
);

export const purgeDeletedAccounts = onSchedule(
  {region: REGION, schedule: "every day 03:00", timeZone: "UTC", maxInstances: 1},
  async () => {
    for (;;) {
      const due = await db.collection(Collections.users)
        .where("purgeAfter", "<=", Timestamp.now())
        .limit(100)
        .get();
      if (due.empty) return;
      for (const doc of due.docs) {
        try {
          await hardDeleteAccount(doc.id);
          console.log(`purgeDeletedAccounts: erased ${doc.id}`);
        } catch (err) {
          console.error(`purgeDeletedAccounts: failed for ${doc.id}`, err);
          return; // retry tomorrow; avoids looping on a doc that keeps failing
        }
      }
    }
  }
);
