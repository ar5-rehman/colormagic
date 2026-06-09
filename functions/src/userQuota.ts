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
import {ENFORCE_APP_CHECK, REGION} from "./config";
import {
  clampOffsetMinutes,
  computeQuota,
  ensureUserDoc,
  grantDailyCreditsIfNeeded,
  setSubscriptionState,
  updateStreakForUser,
} from "./credits";
import {fetchSubscriptionState} from "./playApi";
import {UserDoc, UserQuotaResponse} from "./types";

/**
 * Backstop for premium expiry: if the user is "pro" but the paid period has
 * elapsed, re-check the real state with Google Play. This makes premium end
 * exactly when the paid period does — even if RTDN is not configured or a
 * notification was missed — while never revoking early (a cancelled-but-not-
 * expired sub stays active). Only hits the Play API once per cycle (when the
 * stored expiry has passed), and FAILS OPEN so a Play hiccup can't lock a
 * paying user out.
 */
async function revalidateSubscriptionIfExpired(
  uid: string,
  user: UserDoc,
  offset: number
): Promise<UserDoc> {
  const expired =
    user.plan === "pro" &&
    user.subscriptionActive &&
    !!user.subscriptionExpiresAt &&
    Date.now() > user.subscriptionExpiresAt.toMillis();
  if (!expired || !user.subscriptionPurchaseToken) return user;

  try {
    const state = await fetchSubscriptionState(user.subscriptionPurchaseToken);
    await setSubscriptionState(uid, state.active, state.expiresAt);
    return await ensureUserDoc(uid, offset); // re-read the updated doc
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error("subscription re-validation failed; keeping state", err);
    return user;
  }
}

export const userQuota = onCall(
  {region: REGION, enforceAppCheck: ENFORCE_APP_CHECK},
  async (request): Promise<UserQuotaResponse> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign-in required.");
    }
    // eslint-disable-next-line no-console
    console.log(`userQuota: invoked for uid=${uid} (appCheck=${request.app != null})`);
    // The client sends its timezone offset so daily credits reset at the
    // user's LOCAL midnight (anchored to the server clock — not gameable).
    const offset = clampOffsetMinutes(
      (request.data as {utcOffsetMinutes?: number} | undefined)?.utcOffsetMinutes
    );
    // ensureUserDoc creates the doc on first access; grantDailyCreditsIfNeeded
    // applies any pending daily/monthly resets and persists them.
    await ensureUserDoc(uid, offset);
    let user = await grantDailyCreditsIfNeeded(uid, offset);
    user = await revalidateSubscriptionIfExpired(uid, user, offset);
    // Advance the consecutive-day coloring streak (idempotent within a day).
    const {user: withStreak, advancedToday} = await updateStreakForUser(uid, offset);
    return {...computeQuota(withStreak), streakAdvancedToday: advancedToday};
  }
);
