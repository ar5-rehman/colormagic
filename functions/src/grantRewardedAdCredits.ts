/**
 * Callable: grantRewardedAdCredits
 *
 * Called by the Android client ONLY after the AdMob rewarded-ad completion
 * callback fires with a confirmed reward. Grants REWARDED_AD_CREDITS to the
 * user's FREE adCredits bucket (fulfilled by the free image provider, never
 * OpenAI), subject to the MAX_REWARDED_ADS_PER_DAY cap.
 *
 * Security: credits are never granted client-side. The client just reports
 * that the ad completed; the server decides whether to honour it.
 *
 * Note: Because this function cannot independently verify that the user
 * actually watched an ad (AdMob has no server-side callback in this flow),
 * this is best-effort trust. For higher assurance, use AdMob's server-side
 * verification (SSV) callback and call this function from there instead.
 */
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {
  REGION,
  REWARDED_AD_CREDITS,
  MAX_REWARDED_ADS_PER_DAY,
  USE_ADMOB_SSV,
} from "./config";
import {
  clampOffsetMinutes,
  computeQuota,
  ensureUserDoc,
  grantDailyCreditsIfNeeded,
  grantRewardedAdCreditsForUser,
} from "./credits";

interface GrantRewardedAdResponse {
  success: boolean;
  creditsGranted: number;
  newBalance: number;
  rewardedAdsToday: number;
  rewardedAdsRemaining: number;
  errorCode?: string;
}

export const grantRewardedAdCredits = onCall(
  {region: REGION, enforceAppCheck: true},
  async (request): Promise<GrantRewardedAdResponse> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign-in required.");
    }

    const offset = clampOffsetMinutes(
      (request.data as {utcOffsetMinutes?: number} | undefined)?.utcOffsetMinutes
    );
    await ensureUserDoc(uid, offset);

    // When AdMob SSV is the authoritative grant path, the client must NOT also
    // grant via this callable (that would double-credit). Refuse to grant and
    // just echo the current balance; the SSV callback does the real work.
    if (USE_ADMOB_SSV) {
      const user = await grantDailyCreditsIfNeeded(uid, offset);
      const q = computeQuota(user);
      return {
        success: false,
        creditsGranted: 0,
        newBalance: q.totalAvailableCredits,
        rewardedAdsToday: q.rewardedAdsToday,
        rewardedAdsRemaining: q.rewardedAdsRemaining,
        errorCode: "ssv_authoritative",
      };
    }

    const result = await grantRewardedAdCreditsForUser(uid, offset);

    if (result.error === "daily_ad_limit_reached") {
      return {
        success: false,
        creditsGranted: 0,
        newBalance: result.newBalance,
        rewardedAdsToday: result.rewardedAdsToday,
        rewardedAdsRemaining: 0,
        errorCode: "daily_ad_limit_reached",
      };
    }

    return {
      success: true,
      creditsGranted: REWARDED_AD_CREDITS,
      newBalance: result.newBalance,
      rewardedAdsToday: result.rewardedAdsToday,
      rewardedAdsRemaining: Math.max(
        0,
        MAX_REWARDED_ADS_PER_DAY - result.rewardedAdsToday
      ),
    };
  }
);
