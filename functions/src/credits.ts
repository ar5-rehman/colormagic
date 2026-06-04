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

/** Returns today's date as "YYYY-MM-DD" in UTC. (Kept for legacy callers.) */
export function todayUtc(): string {
  return new Date().toISOString().slice(0, 10);
}

/** Largest plausible real-world timezone offset (UTC±14:00), in minutes. */
const MAX_OFFSET_MINUTES = 14 * 60;

/**
 * Sanitises a client-supplied UTC offset (minutes to ADD to UTC to reach the
 * device's local time; e.g. UTC+5 → 300, US-Pacific → -480). Non-numeric or
 * out-of-range values fall back to 0 (UTC). Clamping means a spoofed offset can
 * at most shift the day boundary by ±14h — it can never create a grant loop.
 */
export function clampOffsetMinutes(value: unknown): number {
  const n = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(n)) return 0;
  return Math.max(-MAX_OFFSET_MINUTES, Math.min(MAX_OFFSET_MINUTES, Math.trunc(n)));
}

/**
 * The user's LOCAL calendar day ("YYYY-MM-DD") for a given instant, computed
 * from the (trusted, server-supplied) epoch millis plus the client's timezone
 * offset. Because the instant always originates from the server clock, changing
 * the device clock has no effect — only the timezone offset moves the boundary.
 */
export function localDayKey(epochMillis: number, offsetMinutes: number): string {
  return new Date(epochMillis + offsetMinutes * 60_000).toISOString().slice(0, 10);
}

const userRef = (uid: string) => db.collection(Collections.users).doc(uid);

/** Loads the user doc, creating a fresh free-tier doc on first access. */
export async function ensureUserDoc(uid: string, offsetMinutes = 0): Promise<UserDoc> {
  const ref = userRef(uid);
  const snap = await ref.get();
  if (snap.exists) return normalizeUser(snap.data() as UserDoc, offsetMinutes);

  const now = Timestamp.now();
  const today = localDayKey(now.toMillis(), clampOffsetMinutes(offsetMinutes));
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
    lastDailyGrantAt: now,
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
 *   1. Daily credit refresh  — if the user's LOCAL day rolled over since the
 *                              last grant, replenish the daily bucket
 *   2. Rewarded-ad reset     — same local-day boundary, zero the counter
 *   3. Pro monthly reset     — if billing window elapsed, zero usedThisMonth
 *
 * `offsetMinutes` is the client's timezone offset (minutes to ADD to UTC). The
 * day boundary is the user's local midnight, but the comparison uses the server
 * clock (Date.now()), so a tampered device clock cannot trigger extra grants.
 *
 * The caller is responsible for persisting mutations back to Firestore.
 */
export function normalizeUser(user: UserDoc, offsetMinutes = 0): UserDoc {
  const offset = clampOffsetMinutes(offsetMinutes);
  const nowMs = Date.now();
  const todayKey = localDayKey(nowMs, offset);
  let result = {...user};

  // ── Daily credit refresh (local-day boundary, server-clock anchored) ───
  // Prefer the trusted timestamp; fall back to the stored day string for
  // legacy docs created before lastDailyGrantAt existed.
  const lastGrantKey = result.lastDailyGrantAt
    ? localDayKey(result.lastDailyGrantAt.toMillis(), offset)
    : result.dailyCreditsDate ?? null;
  if (lastGrantKey !== todayKey) {
    const isPro = result.plan === "pro" && result.subscriptionActive;
    result = {
      ...result,
      dailyCreditsDate: todayKey,
      dailyCreditsAvailable: isPro ? PREMIUM_DAILY_CREDITS : FREE_DAILY_CREDITS,
      lastDailyGrantAt: Timestamp.fromMillis(nowMs),
    };
  }

  // ── Rewarded-ad counter reset ─────────────────────────────────────────
  if (result.rewardedAdsDate !== todayKey) {
    result = {
      ...result,
      rewardedAdsDate: todayKey,
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
 * for the same local day.
 */
export async function grantDailyCreditsIfNeeded(
  uid: string,
  offsetMinutes = 0
): Promise<UserDoc> {
  const ref = userRef(uid);
  const snap = await ref.get();
  if (!snap.exists) return ensureUserDoc(uid, offsetMinutes);

  const raw = snap.data() as UserDoc;
  const normalized = normalizeUser(raw, offsetMinutes);

  // Only write if something actually changed (avoids unnecessary writes)
  const rawGrantMs = raw.lastDailyGrantAt ? raw.lastDailyGrantAt.toMillis() : null;
  const normGrantMs =
    normalized.lastDailyGrantAt ? normalized.lastDailyGrantAt.toMillis() : null;
  const monthlyChanged =
    normalized.usedSketchesThisMonth !== raw.usedSketchesThisMonth;

  if (
    normalized.dailyCreditsDate !== raw.dailyCreditsDate ||
    normalized.dailyCreditsAvailable !== raw.dailyCreditsAvailable ||
    normGrantMs !== rawGrantMs ||
    normalized.rewardedAdsDate !== raw.rewardedAdsDate ||
    monthlyChanged
  ) {
    await ref.update({
      dailyCreditsDate: normalized.dailyCreditsDate,
      dailyCreditsAvailable: normalized.dailyCreditsAvailable,
      lastDailyGrantAt: normalized.lastDailyGrantAt ?? null,
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
  uid: string,
  offsetMinutes = 0
): Promise<{error: string | null; newBalance: number; rewardedAdsToday: number}> {
  let newBalance = 0;
  let newAdsToday = 0;
  let error: string | null = null;

  await db.runTransaction(async (tx) => {
    const ref = userRef(uid);
    const snap = await tx.get(ref);
    if (!snap.exists) throw new Error(`User doc ${uid} not found`);

    const user = normalizeUser(snap.data() as UserDoc, offsetMinutes);
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
      rewardedAdsDate: user.rewardedAdsDate,
      // Persist any lazy daily reset that happened inside normalizeUser
      dailyCreditsDate: user.dailyCreditsDate,
      dailyCreditsAvailable: user.dailyCreditsAvailable,
      lastDailyGrantAt: user.lastDailyGrantAt ?? null,
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
  sketch: Omit<SketchDoc, "createdAt">,
  offsetMinutes = 0
): Promise<void> {
  await db.runTransaction(async (tx) => {
    const ref = userRef(uid);
    const snap = await tx.get(ref);
    if (!snap.exists) throw new Error(`User doc ${uid} vanished mid-transaction`);

    const user = normalizeUser(snap.data() as UserDoc, offsetMinutes);
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
      lastDailyGrantAt: user.lastDailyGrantAt ?? null,
      rewardedAdsDate: user.rewardedAdsDate,
      rewardedAdsToday: user.rewardedAdsToday,
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
