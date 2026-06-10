/** Shared Firestore document shapes and callable request/response types. */
import {Timestamp} from "firebase-admin/firestore";

export type Plan = "free" | "pro";

/** Firestore: users/{uid} */
export interface UserDoc {
  plan: Plan;
  subscriptionActive: boolean;
  // Legacy lifetime-free fields (kept for migration; no longer the primary gate)
  freeSketchesTotal: number;
  freeSketchesUsed: number;
  // Pro subscription monthly allowance
  monthlySketchLimit: number;
  usedSketchesThisMonth: number;
  /** When usedSketchesThisMonth rolls back to 0. Null for free accounts. */
  monthlyResetAt: Timestamp | null;
  /** End of the currently-paid subscription period (from Google Play). Premium
   *  access continues until this instant even after the user cancels auto-renew.
   *  Distinct from monthlyResetAt (which advances each usage cycle). */
  subscriptionExpiresAt: Timestamp | null;
  /** The active subscription's Play purchaseToken — lets the backend re-verify
   *  the real state at expiry. Null for free accounts. */
  subscriptionPurchaseToken: string | null;
  // Purchased extra packs (PAID — these use the OpenAI provider).
  extraCredits: number;
  // Rewarded-ad earned credits (FREE — these always use the free Cloudflare
  // provider, never OpenAI, so ad rewards never cost us money to fulfil).
  adCredits: number;
  // Daily free/premium credit grant — refreshed once per the USER'S LOCAL
  // calendar day. The boundary is the user's local midnight, but the decision
  // is anchored to the trusted server clock (see credits.normalizeUser), so
  // changing the device clock cannot farm extra grants.
  dailyCreditsDate: string | null;   // "YYYY-MM-DD" local day of last grant
  dailyCreditsAvailable: number;     // remaining daily credits for today
  /** Server timestamp of the last daily grant — the trusted anchor for the
   *  local-day reset check. Null only on legacy docs created before this field. */
  lastDailyGrantAt: Timestamp | null;
  // Rewarded-ad rate limiting — resets on the same local-day boundary
  rewardedAdsDate: string | null;    // "YYYY-MM-DD" local day
  rewardedAdsToday: number;          // ads watched today
  // ── Coloring streak (consecutive local days the app was opened) ──
  // Stored on the user doc so it follows a Google account across devices.
  streakCurrent?: number;            // current consecutive-day streak
  streakBest?: number;               // best streak ever reached
  streakLastDay?: number | null;     // local day-number of last activity
  // ── User profile (populated from Google sign-in, null for guests) ──
  displayName?: string | null;
  email?: string | null;
  photoUrl?: string | null;
  // ── Parent controls (synced across devices for Google users) ──
  parentDailySketchLimit?: number | null;   // null = unlimited
  parentAllowFreeText?: boolean;
  parentSessionLimitMinutes?: number | null; // null = off
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

/** Credit transaction types for audit / analytics */
export type CreditTransactionType =
  | "daily_grant"
  | "rewarded_ad_grant"
  | "generation_spend"
  | "premium_grant"
  | "admin_adjustment";

/** Firestore: sketches/{sketchId} */
export interface SketchDoc {
  sketchId: string;
  userId: string;
  prompt: string;
  model: string;
  quality: string;
  imageUrl: string;
  storagePath: string;
  createdAt: Timestamp;
}

// ── Callable: generateSketch ───────────────────────────────────────────
export interface GenerateSketchRequest {
  prompt: string;
}
export interface GenerateSketchResponse {
  success: true;
  sketchId: string;
  imageUrl: string;
  /** True when this sketch advanced the coloring streak to a new day. */
  streakAdvancedToday: boolean;
}

// ── Callable: userQuota ────────────────────────────────────────────────
export interface UserQuotaResponse {
  plan: Plan;
  subscriptionActive: boolean;
  remainingFreeSketches: number;      // today's daily credits remaining
  remainingMonthlySketches: number;
  extraCredits: number;               // purchased (paid) credits
  adCredits: number;                  // rewarded-ad (free) credits
  totalAvailableCredits: number;
  rewardedAdsToday: number;
  rewardedAdsRemaining: number;
  // Coloring streak — current/best consecutive days, and whether THIS call
  // advanced it to a new day (so the client can fire a one-time celebration).
  streakCurrent: number;
  streakBest: number;
  streakAdvancedToday: boolean;
  // Parent controls — synced to Firestore so they follow the Google account.
  parentDailySketchLimit: number | null;    // null = unlimited
  parentAllowFreeText: boolean;
  parentSessionLimitMinutes: number | null; // null = off
}

// ── Callable: verifyPurchase ───────────────────────────────────────────
export interface VerifyPurchaseRequest {
  productId: string;
  purchaseToken: string;
}
export interface VerifyPurchaseResponse {
  success: true;
  plan: Plan;
  totalAvailableCredits: number;
}
