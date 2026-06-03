/**
 * Credit accounting — the heart of the backend's money logic.
 *
 * Credit buckets, in consumption priority:
 *   1. daily     — FREE_DAILY_CREDITS (free) or PREMIUM_DAILY_CREDITS (pro)
 *                  granted once per UTC calendar day
 *   2. monthly   — Pro plan's per-cycle allowance (MODEL_PREMIUM)
 *   3. extra     — purchased extra packs + rewarded-ad grants (never expire)
 *
 * Free users only have daily + extra. Pro users have all three.
 */
import {Timestamp} from "firebase-admin/firestore";
import {db, Collections} from "./firebase";
import {
  FREE_SKETCHES_TOTAL,
  MONTHLY_PERIOD_DAYS,
  MODEL_FREE,
  MODEL_PREMIUM,
  FREE_DAILY_CREDITS,
  PREMIUM_DAILY_CREDITS,
  REWARDED_AD_CREDITS,
  MAX_REWARDED_ADS_PER_DAY,
} from "./config";
import {SketchDoc, UserDoc, UserQuotaResponse} from "./types";

export type CreditSource = "daily" | "monthly" | "extra";

/** Returns today's date as "YYYY-MM-DD" in UTC. */
export function todayUtc(): string {
  return new Date().toISOString().slice(0, 10);
}

const userRef = (uid: string) => db.collection(Collections.users).doc(uid);

/** Loads the user doc, creating a fresh free-tier doc on first access. */
export async function ensureUserDoc(uid: string): Promise<UserDoc> {
  const ref = userRef(uid);
  const snap = await ref.get();
  if (snap.exists) return normalizeUser(snap.data() as UserDoc);

  const now = Timestamp.now();
  const today = todayUtc();
  const fresh: UserDoc = {
    plan: "free",
    subscriptionActive: false,
    freeSketchesTotal: FREE_SKETCHES_TOTAL,
    freeSketchesUsed: 0,
    monthlySketchLimit: 0,
    usedSketchesThisMonth: 0,
    extraCredits: 0,
    monthlyResetAt: null,
    // New daily credit fields — grant the first day's credits immediately
    dailyCreditsDate: today,
    dailyCreditsAvailable: FREE_DAILY_CREDITS,
    rewardedAdsDate: today,
    rewardedAdsToday: 0,
    createdAt: now,
    updatedAt: now,
  };
  await ref.set(fresh);
  return fresh;
}

/**
 * Applies all lazy resets in-memory (no Firestore write here):
 *   1. Daily credit refresh  — if today ≠ dailyCreditsDate, replenish daily bucket
 *   2. Rewarded-ad reset     — if today ≠ rewardedAdsDate, zero the counter
 *   3. Pro monthly reset     — if billing window elapsed, zero usedThisMonth
 *
 * The caller is responsible for persisting mutations back to Firestore.
 */
export function normalizeUser(user: UserDoc): UserDoc {
  const today = todayUtc();
  let result = {...user};

  // ── Daily credit refresh ──────────────────────────────────────────────
  if (result.dailyCreditsDate !== today) {
    const isPro = result.plan === "pro" && result.subscriptionActive;
    result = {
      ...result,
      dailyCreditsDate: today,
      dailyCreditsAvailable: isPro ? PREMIUM_DAILY_CREDITS : FREE_DAILY_CREDITS,
    };
  }

  // ── Rewarded-ad counter reset ─────────────────────────────────────────
  if (result.rewardedAdsDate !== today) {
    result = {
      ...result,
      rewardedAdsDate: today,
      rewardedAdsToday: 0,
    };
  }

  // ── Pro monthly reset (lazy) ──────────────────────────────────────────
  const isPro = result.plan === "pro" && result.subscriptionActive;
  if (isPro && result.monthlyResetAt && Date.now() >= result.monthlyResetAt.toMillis()) {
    result = {
      ...result,
      usedSketchesThisMonth: 0,
      monthlyResetAt: Timestamp.fromMillis(
        Date.now() + MONTHLY_PERIOD_DAYS * 24 * 60 * 60 * 1000
      ),
    };
  }

  return result;
}

/** Computes the public-facing quota snapshot returned by `userQuota`. */
export function computeQuota(user: UserDoc): UserQuotaResponse {
  const isPro = user.plan === "pro" && user.subscriptionActive;
  const daily = Math.max(0, user.dailyCreditsAvailable ?? 0);
  const remainingMonthly = isPro
    ? Math.max(0, user.monthlySketchLimit - user.usedSketchesThisMonth)
    : 0;
  const extra = Math.max(0, user.extraCredits);
  return {
    plan: user.plan,
    subscriptionActive: user.subscriptionActive,
    remainingFreeSketches: daily,          // repurposed: now means "today's daily"
    remainingMonthlySketches: remainingMonthly,
    extraCredits: extra,
    totalAvailableCredits: daily + remainingMonthly + extra,
    rewardedAdsToday: user.rewardedAdsToday ?? 0,
    rewardedAdsRemaining: Math.max(0, MAX_REWARDED_ADS_PER_DAY - (user.rewardedAdsToday ?? 0)),
  };
}

/** Which bucket the next sketch draws from — null if out of credits. */
export function pickCreditSource(user: UserDoc): CreditSource | null {
  const q = computeQuota(user);
  if (q.remainingFreeSketches > 0) return "daily";
  if (q.remainingMonthlySketches > 0) return "monthly";
  if (q.extraCredits > 0) return "extra";
  return null;
}

/** The OpenAI model that a credit from [source] unlocks. */
export function modelForSource(source: CreditSource): string {
  return source === "daily" ? MODEL_FREE : MODEL_PREMIUM;
}

export type ImageProvider = "openai" | "pollinations";
export function providerForSource(source: CreditSource): ImageProvider {
  return source === "daily" ? "pollinations" : "openai";
}

/**
 * Grants today's daily credits if not yet granted today, then persists the
 * normalized user doc. Safe to call on every `userQuota` fetch — idempotent
 * for the same UTC day.
 */
export async function grantDailyCreditsIfNeeded(uid: string): Promise<UserDoc> {
  const ref = userRef(uid);
  const snap = await ref.get();
  if (!snap.exists) return ensureUserDoc(uid);

  const raw = snap.data() as UserDoc;
  const normalized = normalizeUser(raw);

  // Only write if something actually changed (avoids unnecessary writes)
  const daily = normalized.dailyCreditsDate;
  const adDate = normalized.rewardedAdsDate;
  const monthlyChanged =
    normalized.usedSketchesThisMonth !== raw.usedSketchesThisMonth;

  if (
    daily !== raw.dailyCreditsDate ||
    normalized.dailyCreditsAvailable !== raw.dailyCreditsAvailable ||
    adDate !== raw.rewardedAdsDate ||
    monthlyChanged
  ) {
    await ref.update({
      dailyCreditsDate: normalized.dailyCreditsDate,
      dailyCreditsAvailable: normalized.dailyCreditsAvailable,
      rewardedAdsDate: normalized.rewardedAdsDate,
      rewardedAdsToday: normalized.rewardedAdsToday,
      usedSketchesThisMonth: normalized.usedSketchesThisMonth,
      monthlyResetAt: normalized.monthlyResetAt,
      updatedAt: Timestamp.now(),
    });
  }
  return normalized;
}

/**
 * Grants REWARDED_AD_CREDITS to extraCredits and increments rewardedAdsToday.
 * Returns an error string if the daily cap is already reached, otherwise null.
 *
 * Runs in a transaction so concurrent ad completions don't double-grant.
 */
export async function grantRewardedAdCreditsForUser(
  uid: string
): Promise<{error: string | null; newBalance: number; rewardedAdsToday: number}> {
  let newBalance = 0;
  let newAdsToday = 0;
  let error: string | null = null;

  await db.runTransaction(async (tx) => {
    const ref = userRef(uid);
    const snap = await tx.get(ref);
    if (!snap.exists) throw new Error(`User doc ${uid} not found`);

    const user = normalizeUser(snap.data() as UserDoc);
    const adsToday = user.rewardedAdsToday ?? 0;

    if (adsToday >= MAX_REWARDED_ADS_PER_DAY) {
      error = "daily_ad_limit_reached";
      newBalance = computeQuota(user).totalAvailableCredits;
      newAdsToday = adsToday;
      return; // no changes
    }

    const updated = {
      extraCredits: (user.extraCredits ?? 0) + REWARDED_AD_CREDITS,
      rewardedAdsToday: adsToday + 1,
      rewardedAdsDate: todayUtc(),
      // Persist any lazy daily reset that happened inside normalizeUser
      dailyCreditsDate: user.dailyCreditsDate,
      dailyCreditsAvailable: user.dailyCreditsAvailable,
      updatedAt: Timestamp.now(),
    };
    tx.update(ref, updated);

    newBalance =
      (user.dailyCreditsAvailable ?? 0) +
      Math.max(0, user.monthlySketchLimit - user.usedSketchesThisMonth) +
      updated.extraCredits;
    newAdsToday = updated.rewardedAdsToday;
  });

  return {error, newBalance, rewardedAdsToday: newAdsToday};
}

/**
 * Atomically deducts one credit AND writes the sketch document in a single
 * transaction — a sketch is recorded if and only if a credit is spent.
 * Call this only AFTER the image is generated and uploaded.
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

    let {dailyCreditsAvailable, usedSketchesThisMonth, extraCredits} = user;
    if (source === "daily") dailyCreditsAvailable = Math.max(0, dailyCreditsAvailable - 1);
    else if (source === "monthly") usedSketchesThisMonth += 1;
    else if (source === "extra") extraCredits = Math.max(0, extraCredits - 1);
    else {
      // eslint-disable-next-line no-console
      console.warn(`commitSketch: ${uid} had no credit at deduction time`);
    }

    tx.update(ref, {
      dailyCreditsAvailable,
      dailyCreditsDate: user.dailyCreditsDate,
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
