/**
 * Per-PHONE free-credit limits.
 *
 * Each phone has a devices/{deviceKey} doc (see deviceAccount.ts) that
 * remembers the account currently used on it. These helpers also record, per
 * phone, the FIRST account created on it (devices/{key}.firstUid) and how many
 * rewarded ads were watched today — so only a phone's first account ever gets
 * free daily credits, and a new account can't restart the ad allowance.
 * Premium daily credits are paid for and not limited.
 */
import type {DocumentSnapshot} from "firebase-admin/firestore";
import {Collections, db} from "./firebase";

/** The devices/{key} doc whose current account is [uid], if any. */
export async function deviceDocForUid(uid: string): Promise<DocumentSnapshot | null> {
  const snap = await db.collection(Collections.devices).where("uid", "==", uid).limit(1).get();
  return snap.empty ? null : snap.docs[0];
}

/**
 * For a NEW account being created on a phone:
 *  • freeCreditsEligible — free daily credits are only for the FIRST account
 *    ever created on a phone. Accounts made later on the same phone (after
 *    signing out of Google, deleting an account, or reinstalling without a
 *    restore) start with nothing free.
 *  • adsWatched — continues the phone's rewarded-ad count for today.
 */
export async function deviceUsageToday(
  uid: string,
  dayKey: string
): Promise<{freeCreditsEligible: boolean; adsWatched: number}> {
  const dev = await deviceDocForUid(uid).catch(() => null);
  if (!dev) return {freeCreditsEligible: true, adsWatched: 0};
  const firstUid = dev.get("firstUid") as string | undefined;
  return {
    freeCreditsEligible: !firstUid || firstUid === uid,
    adsWatched: dev.get("adsDay") === dayKey ? (dev.get("adsCount") as number ?? 0) : 0,
  };
}

/** Records the phone's rewarded-ad count for [dayKey]. */
export async function noteAdsWatched(uid: string, dayKey: string, count: number): Promise<void> {
  const dev = await deviceDocForUid(uid).catch(() => null);
  await dev?.ref.set({adsDay: dayKey, adsCount: count}, {merge: true}).catch(() => undefined);
}
