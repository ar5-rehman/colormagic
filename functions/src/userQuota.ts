/**
 * Callable: userQuota
 *
 * Returns the caller's credit snapshot. Also performs the lazy daily
 * credit grant — if the UTC date has rolled over since the last grant, the
 * daily bucket is replenished before the snapshot is computed and returned.
 * This means a single quota fetch is always enough to unblock a new day's
 * worth of sketches without a separate "reset" call.
 */
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {REGION} from "./config";
import {
  clampOffsetMinutes,
  computeQuota,
  ensureUserDoc,
  grantDailyCreditsIfNeeded,
} from "./credits";
import {UserQuotaResponse} from "./types";

export const userQuota = onCall(
  {region: REGION, enforceAppCheck: true},
  async (request): Promise<UserQuotaResponse> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign-in required.");
    }
    // The client sends its timezone offset so daily credits reset at the
    // user's LOCAL midnight (anchored to the server clock — not gameable).
    const offset = clampOffsetMinutes(
      (request.data as {utcOffsetMinutes?: number} | undefined)?.utcOffsetMinutes
    );
    // ensureUserDoc creates the doc on first access; grantDailyCreditsIfNeeded
    // applies any pending daily/monthly resets and persists them.
    await ensureUserDoc(uid, offset);
    const user = await grantDailyCreditsIfNeeded(uid, offset);
    return computeQuota(user);
  }
);
