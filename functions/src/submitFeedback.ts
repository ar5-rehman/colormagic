/**
 * Callable: submitFeedback
 *
 * Receives a suggestion / bug report / question from the parent-side Support
 * screen and stores it in the private `feedback` collection for the developer
 * to review. App Check + Auth enforced; writes are server-only (clients can
 * never read or tamper with the feedback ledger — see firestore.rules).
 *
 * To get notified of new feedback you can add a Firestore onCreate trigger on
 * the `feedback` collection (e.g. email yourself or post to Slack).
 */
import {Timestamp} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {ENFORCE_APP_CHECK, REGION} from "./config";
import {Collections, db} from "./firebase";

const VALID_TYPES = ["suggestion", "bug", "question"] as const;
const MAX_MESSAGE = 4000;
const MAX_SHORT = 300;

interface SubmitFeedbackRequest {
  type?: string;
  message?: string;
  email?: string;
  appVersion?: string;
  device?: string;
  platform?: string;
}

export const submitFeedback = onCall(
  {region: REGION, enforceAppCheck: ENFORCE_APP_CHECK},
  async (request): Promise<{success: true; id: string}> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign-in required.");
    }

    const data = (request.data ?? {}) as SubmitFeedbackRequest;
    const type = (data.type ?? "").toLowerCase();
    const message = (data.message ?? "").trim();

    if (!(VALID_TYPES as readonly string[]).includes(type)) {
      throw new HttpsError("invalid-argument", "Invalid feedback type.");
    }
    if (!message) {
      throw new HttpsError("invalid-argument", "Please enter a message.");
    }
    if (message.length > MAX_MESSAGE) {
      throw new HttpsError("invalid-argument", "Message is too long.");
    }

    const email = (data.email ?? "").trim().slice(0, MAX_SHORT);
    const doc = {
      uid,
      type,
      message,
      email: email || null,
      appVersion: (data.appVersion ?? "").slice(0, MAX_SHORT),
      device: (data.device ?? "").slice(0, MAX_SHORT),
      platform: (data.platform ?? "android").slice(0, MAX_SHORT),
      status: "new",
      createdAt: Timestamp.now(),
    };

    const ref = await db.collection(Collections.feedback).add(doc);
    return {success: true, id: ref.id};
  }
);
