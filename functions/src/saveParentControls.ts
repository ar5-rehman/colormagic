/**
 * Callable: saveParentControls
 *
 * Persists parent-control settings to the user's Firestore doc so they
 * sync across devices when the child is signed in with Google. Guest
 * users can call this too (their controls live on the doc tied to the
 * anonymous uid), but the data only survives as long as the guest account
 * does — which is surfaced in the UI as a "sign in to keep your data"
 * prompt.
 */
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {Timestamp} from "firebase-admin/firestore";
import {ENFORCE_APP_CHECK, REGION} from "./config";
import {db, Collections} from "./firebase";

interface SaveParentControlsRequest {
  dailySketchLimit?: number | null;    // null = unlimited
  allowFreeText?: boolean;
  sessionLimitMinutes?: number | null;  // null = off
}

export const saveParentControls = onCall(
  {region: REGION, enforceAppCheck: ENFORCE_APP_CHECK},
  async (request): Promise<{success: true}> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign-in required.");
    }
    const data = request.data as SaveParentControlsRequest | undefined;
    if (!data) {
      throw new HttpsError("invalid-argument", "No data provided.");
    }

    const updates: Record<string, unknown> = {updatedAt: Timestamp.now()};

    if (data.dailySketchLimit !== undefined) {
      // Validate: must be null (unlimited) or a positive integer.
      const v = data.dailySketchLimit;
      if (v !== null && (typeof v !== "number" || v < 1 || v > 99)) {
        throw new HttpsError("invalid-argument", "dailySketchLimit must be 1–99 or null.");
      }
      updates.parentDailySketchLimit = v;
    }
    if (data.allowFreeText !== undefined) {
      updates.parentAllowFreeText = !!data.allowFreeText;
    }
    if (data.sessionLimitMinutes !== undefined) {
      const v = data.sessionLimitMinutes;
      if (v !== null && (typeof v !== "number" || v < 5 || v > 240)) {
        throw new HttpsError("invalid-argument", "sessionLimitMinutes must be 5–240 or null.");
      }
      updates.parentSessionLimitMinutes = v;
    }

    await db.collection(Collections.users).doc(uid).update(updates);
    return {success: true};
  }
);
