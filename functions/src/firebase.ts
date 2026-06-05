/**
 * Firebase Admin SDK initialization. Imported (for its side effect) by every
 * function module so `db` / `storage` are ready. Admin SDK calls run with
 * full privileges and bypass Firestore/Storage security rules.
 */
import {initializeApp} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";
import {getStorage} from "firebase-admin/storage";

initializeApp();

export const db = getFirestore();
export const storage = getStorage();

/** Firestore collection names — single source of truth. */
export const Collections = {
  users: "users",
  sketches: "sketches",
  /** Idempotency ledger — one doc per processed Play purchaseToken. */
  processedPurchases: "processedPurchases",
  /** Idempotency ledger — one doc per AdMob SSV transaction_id. */
  processedAdRewards: "processedAdRewards",
  /** Parent-submitted feedback (suggestions / bug reports / questions). */
  feedback: "feedback",
} as const;
