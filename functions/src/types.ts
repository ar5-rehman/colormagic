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
  // Purchased extra packs + rewarded-ad grants share this bucket
  extraCredits: number;
  // Daily free/premium credit grant — refreshed once per calendar day (UTC)
  dailyCreditsDate: string | null;   // "YYYY-MM-DD" UTC, null on first create
  dailyCreditsAvailable: number;     // remaining daily credits for today
  // Rewarded-ad rate limiting — resets daily
  rewardedAdsDate: string | null;    // "YYYY-MM-DD" UTC
  rewardedAdsToday: number;          // ads watched today
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
}

// ── Callable: userQuota ────────────────────────────────────────────────
export interface UserQuotaResponse {
  plan: Plan;
  subscriptionActive: boolean;
  remainingFreeSketches: number;      // today's daily credits remaining
  remainingMonthlySketches: number;
  extraCredits: number;
  totalAvailableCredits: number;
  rewardedAdsToday: number;
  rewardedAdsRemaining: number;
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
