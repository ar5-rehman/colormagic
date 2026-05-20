/**
 * Credit accounting — the heart of the backend's money logic.
 *
 * Buckets, in consumption priority:
 *   1. free     — lifetime free sketches (free plan only)   → MODEL_FREE
 *   2. monthly  — Pro plan's per-cycle allowance            → MODEL_PREMIUM
 *   3. extra    — purchased extra packs, never expire       → MODEL_PREMIUM
 *
 * The model a sketch uses depends on the BUCKET the credit comes from, not
 * the user's plan — a free-plan user who buys an extra pack gets the premium
 * model when spending those extra credits.
 */
import {Timestamp} from "firebase-admin/firestore";
import {db, Collections} from "./firebase";
import {
  FREE_SKETCHES_TOTAL,
  MONTHLY_PERIOD_DAYS,
  MODEL_FREE,
  MODEL_PREMIUM,
} from "./config";
import {SketchDoc, UserDoc, UserQuotaResponse} from "./types";

export type CreditSource = "free" | "monthly" | "extra";

const userRef = (uid: string) => db.collection(Collections.users).doc(uid);

/** Loads the user doc, creating a fresh free-tier doc on first access. */
export async function ensureUserDoc(uid: string): Promise<UserDoc> {
  const ref = userRef(uid);
  const snap = await ref.get();
  if (snap.exists) return normalizeUser(snap.data() as UserDoc);

  const now = Timestamp.now();
  const fresh: UserDoc = {
    plan: "free",
    subscriptionActive: false,
    freeSketchesTotal: FREE_SKETCHES_TOTAL,
    freeSketchesUsed: 0,
    monthlySketchLimit: 0,
    usedSketchesThisMonth: 0,
    extraCredits: 0,
    monthlyResetAt: null,
    createdAt: now,
    updatedAt: now,
  };
  await ref.set(fresh);
  return fresh;
}

/**
 * Lazy monthly reset. If the Pro billing window has elapsed we treat
 * usedSketchesThisMonth as 0 and roll the window forward. This keeps the
 * reset correct even if no `verifyPurchase` renewal call has landed yet.
 */
export function normalizeUser(user: UserDoc): UserDoc {
  const isPro = user.plan === "pro" && user.subscriptionActive;
  if (isPro && user.monthlyResetAt && Date.now() >= user.monthlyResetAt.toMillis()) {
    return {
      ...user,
      usedSketchesThisMonth: 0,
      monthlyResetAt: Timestamp.fromMillis(
        Date.now() + MONTHLY_PERIOD_DAYS * 24 * 60 * 60 * 1000
      ),
    };
  }
  return user;
}

/** Computes the public-facing quota snapshot returned by `userQuota`. */
export function computeQuota(user: UserDoc): UserQuotaResponse {
  const isPro = user.plan === "pro" && user.subscriptionActive;
  const remainingFree = isPro
    ? 0
    : Math.max(0, user.freeSketchesTotal - user.freeSketchesUsed);
  const remainingMonthly = isPro
    ? Math.max(0, user.monthlySketchLimit - user.usedSketchesThisMonth)
    : 0;
  const extra = Math.max(0, user.extraCredits);
  return {
    plan: user.plan,
    subscriptionActive: user.subscriptionActive,
    remainingFreeSketches: remainingFree,
    remainingMonthlySketches: remainingMonthly,
    extraCredits: extra,
    totalAvailableCredits: remainingFree + remainingMonthly + extra,
  };
}

/** Which bucket the next sketch would draw from — null if the user is out. */
export function pickCreditSource(user: UserDoc): CreditSource | null {
  const q = computeQuota(user);
  if (q.remainingFreeSketches > 0) return "free";
  if (q.remainingMonthlySketches > 0) return "monthly";
  if (q.extraCredits > 0) return "extra";
  return null;
}

/** The OpenAI model that a credit from [source] unlocks. */
export function modelForSource(source: CreditSource): string {
  return source === "free" ? MODEL_FREE : MODEL_PREMIUM;
}

/**
 * Which image provider a credit from [source] uses.
 *
 *   Free credit       → "pollinations" (zero cost to us, basic quality)
 *   Monthly / Extra   → "openai"       (paid quality, paid for by their sub)
 *
 * This means paid users get the upgrade they paid for the moment their
 * Firestore doc shows their purchase — no separate flag, no code change.
 * If the OpenAI account doesn't have billing yet, override both buckets to
 * "pollinations" via `FORCE_FREE_PROVIDER` in config.
 */
export type ImageProvider = "openai" | "pollinations";
export function providerForSource(source: CreditSource): ImageProvider {
  return source === "free" ? "pollinations" : "openai";
}

/**
 * Atomically deducts one credit AND writes the sketch document, in a single
 * Firestore transaction — so a sketch is recorded if and only if a credit is
 * spent. Call this only AFTER the image is generated and uploaded.
 *
 * Re-reads the user inside the transaction (the pre-check may be stale) and
 * computes absolute counter values rather than FieldValue.increment, which
 * keeps the lazy monthly reset and the deduction from fighting each other.
 *
 * Edge case: if a concurrent request drained the last credit between the
 * pre-check and here, [source] is null — the OpenAI cost is already sunk, so
 * we still record the sketch but skip the deduction and log it.
 */
export async function commitSketchAndDeduct(
  uid: string,
  sketch: Omit<SketchDoc, "createdAt">
): Promise<void> {
  await db.runTransaction(async (tx) => {
    const ref = userRef(uid);
    const snap = await tx.get(ref);
    if (!snap.exists) throw new Error(`User doc ${uid} vanished mid-transaction`);

    const user = normalizeUser(snap.data() as UserDoc);
    const source = pickCreditSource(user);

    let {freeSketchesUsed, usedSketchesThisMonth, extraCredits} = user;
    if (source === "free") freeSketchesUsed += 1;
    else if (source === "monthly") usedSketchesThisMonth += 1;
    else if (source === "extra") extraCredits -= 1;
    else {
      // eslint-disable-next-line no-console
      console.warn(`commitSketch: ${uid} had no credit at deduction time`);
    }

    tx.update(ref, {
      freeSketchesUsed,
      usedSketchesThisMonth,
      extraCredits,
      monthlyResetAt: user.monthlyResetAt,
      updatedAt: Timestamp.now(),
    });
    tx.set(db.collection(Collections.sketches).doc(sketch.sketchId), {
      ...sketch,
      createdAt: Timestamp.now(),
    });
  });
}
