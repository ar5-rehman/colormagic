/** Shared Firestore document shapes and callable request/response types. */
import {Timestamp} from "firebase-admin/firestore";

export type Plan = "free" | "pro";

/** Firestore: users/{uid} */
export interface UserDoc {
  plan: Plan;
  subscriptionActive: boolean;
  freeSketchesTotal: number;
  freeSketchesUsed: number;
  monthlySketchLimit: number;
  usedSketchesThisMonth: number;
  extraCredits: number;
  /** When usedSketchesThisMonth rolls back to 0. Null for free accounts. */
  monthlyResetAt: Timestamp | null;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

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
  remainingFreeSketches: number;
  remainingMonthlySketches: number;
  extraCredits: number;
  totalAvailableCredits: number;
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
