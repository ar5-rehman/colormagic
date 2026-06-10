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
import {Timestamp} from "firebase-admin/firestore";
import {
  clampOffsetMinutes,
  computeQuota,
  ensureUserDoc,
  grantDailyCreditsIfNeeded,
  setSubscriptionState,
} from "./credits";
import {db, Collections} from "./firebase";
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
    const data = request.data as Record<string, unknown> | undefined;
    const offset = clampOffsetMinutes(data?.utcOffsetMinutes);

    // ensureUserDoc creates the doc on first access; grantDailyCreditsIfNeeded
    // applies any pending daily/monthly resets and persists them.
    await ensureUserDoc(uid, offset);
    let user = await grantDailyCreditsIfNeeded(uid, offset);
    user = await revalidateSubscriptionIfExpired(uid, user, offset);

    // Sync profile fields (name, email, photo) if the client sent them.
    // This lets us search users by name/email in the Firestore console.
    // Only written when the value actually changed (avoids unnecessary writes).
    const profileName = typeof data?.displayName === "string" ? data.displayName : null;
    const profileEmail = typeof data?.email === "string" ? data.email : null;
    const profilePhoto = typeof data?.photoUrl === "string" ? data.photoUrl : null;
    const profileUpdates: Record<string, unknown> = {};
    if (profileName && profileName !== user.displayName) {
      profileUpdates.displayName = profileName;
    }
    if (profileEmail && profileEmail !== user.email) {
      profileUpdates.email = profileEmail;
    }
    if (profilePhoto && profilePhoto !== user.photoUrl) {
      profileUpdates.photoUrl = profilePhoto;
    }
    if (Object.keys(profileUpdates).length > 0) {
      profileUpdates.updatedAt = Timestamp.now();
      await db.collection(Collections.users).doc(uid).update(profileUpdates);
    }

    // Streak is READ here (not advanced). It only advances when the child
    // actually creates a sketch — see commitSketchAndDeduct in credits.ts.
    // This prevents "just opening the app" from counting as a streak day.
    return {...computeQuota(user), streakAdvancedToday: false};
  }
);
