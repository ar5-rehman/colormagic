/**
 * Rewarded-ad reporting — a separate Firestore area that answers
 * "are users getting their ad rewards, and how much?" at a glance.
 *
 *  rewardedAdEvents/{autoId}        one row per reward attempt (the raw log)
 *      uid, status, source, creditsGranted, adsToday, maxAdsPerDay,
 *      balanceAfter, isGoogleUser, utcDay, createdAt
 *
 *  rewardedAdUsers/{uid}            running totals per user
 *      totalAdsRewarded, totalCreditsGranted, limitReachedCount,
 *      failedCount, firstRewardAt, lastRewardAt, lastStatus
 *
 *  rewardedAdStats/{YYYY-MM-DD}     daily totals (UTC day) for the whole app
 *      adsRewarded, creditsGranted, limitReached, failed, uniqueUsers
 *      └─ users/{uid}               marker so uniqueUsers counts each user once
 *
 * status: "granted" | "limit_reached" | "already_granted" | "ssv_mode" | "error"
 *
 * All writes are best-effort: a reporting failure is logged and NEVER blocks
 * or undoes the user's reward. Server-only (Admin SDK); clients have no access.
 */
import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {getAuth} from "firebase-admin/auth";
import {MAX_REWARDED_ADS_PER_DAY} from "./config";
import {db} from "./firebase";

export type AdRewardStatus =
  | "granted"
  | "limit_reached"
  | "already_granted"
  | "ssv_mode"
  | "error";

export interface AdRewardRecord {
  uid: string;
  status: AdRewardStatus;
  source: "callable" | "ssv";
  creditsGranted: number;
  adsToday: number;
  balanceAfter: number | null;
  errorMessage?: string;
}

export const AdCollections = {
  events: "rewardedAdEvents",
  users: "rewardedAdUsers",
  daily: "rewardedAdStats",
} as const;

async function isGoogleUser(uid: string): Promise<boolean | null> {
  try {
    const user = await getAuth().getUser(uid);
    return user.providerData.some((p) => p.providerId === "google.com");
  } catch {
    return null;
  }
}

export async function recordAdReward(rec: AdRewardRecord): Promise<void> {
  try {
    const now = Timestamp.now();
    const utcDay = new Date().toISOString().slice(0, 10);
    const granted = rec.status === "granted";
    const limit = rec.status === "limit_reached";
    const failed = rec.status === "error";

    // 1. Raw event log.
    await db.collection(AdCollections.events).add({
      uid: rec.uid,
      status: rec.status,
      source: rec.source,
      creditsGranted: rec.creditsGranted,
      adsToday: rec.adsToday,
      maxAdsPerDay: MAX_REWARDED_ADS_PER_DAY,
      balanceAfter: rec.balanceAfter,
      isGoogleUser: await isGoogleUser(rec.uid),
      errorMessage: rec.errorMessage ?? null,
      utcDay,
      createdAt: now,
    });

    // 2. Per-user running totals.
    const userUpdate: Record<string, unknown> = {
      uid: rec.uid,
      lastStatus: rec.status,
      lastEventAt: now,
      totalAdsRewarded: FieldValue.increment(granted ? 1 : 0),
      totalCreditsGranted: FieldValue.increment(rec.creditsGranted),
      limitReachedCount: FieldValue.increment(limit ? 1 : 0),
      failedCount: FieldValue.increment(failed ? 1 : 0),
    };
    if (granted) userUpdate.lastRewardAt = now;
    const userRef = db.collection(AdCollections.users).doc(rec.uid);
    await userRef.set(userUpdate, {merge: true});
    if (granted) {
      // Set firstRewardAt only the first time this user is rewarded.
      await db.runTransaction(async (tx) => {
        const snap = await tx.get(userRef);
        if (!snap.get("firstRewardAt")) tx.update(userRef, {firstRewardAt: now});
      });
    }

    // 3. Daily app-wide totals (+ unique users that day).
    const dayRef = db.collection(AdCollections.daily).doc(utcDay);
    let newUser = 0;
    if (granted) {
      try {
        await dayRef.collection("users").doc(rec.uid).create({firstAt: now});
        newUser = 1;
      } catch {
        // Already counted today.
      }
    }
    await dayRef.set({
      day: utcDay,
      adsRewarded: FieldValue.increment(granted ? 1 : 0),
      creditsGranted: FieldValue.increment(rec.creditsGranted),
      limitReached: FieldValue.increment(limit ? 1 : 0),
      failed: FieldValue.increment(failed ? 1 : 0),
      uniqueUsers: FieldValue.increment(newUser),
      updatedAt: now,
    }, {merge: true});
  } catch (err) {
    console.warn("recordAdReward failed (reward itself unaffected)", err);
  }
}
