/**
 * Callable: userQuota
 *
 * Returns the caller's remaining credits so the app can show "Sketches
 * left: N". Creates the user document on first call (free tier).
 */
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {REGION} from "./config";
import {computeQuota, ensureUserDoc} from "./credits";
import {UserQuotaResponse} from "./types";

export const userQuota = onCall(
  {region: REGION, enforceAppCheck: true},
  async (request): Promise<UserQuotaResponse> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign-in required.");
    }
    const user = await ensureUserDoc(uid);
    return computeQuota(user);
  }
);
