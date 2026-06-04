/**
 * Credit accounting — the heart of the backend's money logic.
 *
 * Credit buckets, in consumption priority:
 *   1. daily     — FREE_DAILY_CREDITS (free) or PREMIUM_DAILY_CREDITS (pro)
 *                  granted once per local calendar day → FREE provider
 *   2. ad        — earned by watching rewarded ads (never expire) → FREE provider
 *   3. monthly   — Pro plan's per-cycle allowance → PAID provider (OpenAI)
 *   4. extra     — PURCHASED extra packs (never expire) → PAID provider (OpenAI)
 *
 * Economic rule: only PAID credits (monthly, extra) use the costly OpenAI
 * provider. FREE credits (daily, ad) always use the free Pollinations provider,
 * so fulfilling a rewarded-ad reward never costs us money.
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
  PRO_MONTHLY_LIMIT,
} from "./config";
import {SketchDoc, UserDoc, UserQuotaResponse} from "./types";

export type CreditSource = "daily" | "ad" | "monthly" | "extra";

/**
 * Thrown by commitSketchAndDeduct when the user has no credit left at the
 * moment of deduction (e.g. a concurrent request spent the last credit between
 * the pre-check and the commit). Callers map this to a "resource-exhausted"
 * client error. This guarantees a sketch is recorded ONLY if a credit is spent.
 */
export class NoCreditError extends Error {
  constructor() {
    super("no_credit_at_commit");
    this.name = "NoCreditError";
  }
}

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
    adCredits: 0,
    monthlyResetAt: null,
    subscriptionExpiresAt: null,
    subscriptionPurchaseToken: null,
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
  const extra = Math.max(0, user.extraCredits ?? 0);
  const ad = Math.max(0, user.adCredits ?? 0);
  return {
    plan: user.plan,
    subscriptionActive: user.subscriptionActive,
    remainingFreeSketches: daily,          // repurposed: now means "today's daily"
    remainingMonthlySketches: remainingMonthly,
    extraCredits: extra,
    adCredits: ad,
    totalAvailableCredits: daily + ad + remainingMonthly + extra,
    rewardedAdsToday: user.rewardedAdsToday ?? 0,
    rewardedAdsRemaining: Math.max(0, MAX_REWARDED_ADS_PER_DAY - (user.rewardedAdsToday ?? 0)),
  };
}

/** Which bucket the next sketch draws from — null if out of credits.
 *  FREE buckets (daily, ad) are consumed before PAID ones (monthly, extra). */
export function pickCreditSource(user: UserDoc): CreditSource | null {
  const q = computeQuota(user);
  if (q.remainingFreeSketches > 0) return "daily";
  if (q.adCredits > 0) return "ad";
  if (q.remainingMonthlySketches > 0) return "monthly";
  if (q.extraCredits > 0) return "extra";
  return null;
}

/** The image model a credit from [source] unlocks. Free buckets → free model. */
export function modelForSource(source: CreditSource): string {
  return source === "daily" || source === "ad" ? MODEL_FREE : MODEL_PREMIUM;
}

export type ImageProvider = "openai" | "pollinations";

/**
 * Which image service fulfils a credit from [source].
 * FREE credits (daily, ad) → Pollinations (free). PAID credits (monthly, extra)
 * → OpenAI. This guarantees rewarded-ad rewards never cost us OpenAI money.
 */
export function providerForSource(source: CreditSource): ImageProvider {
  return source === "daily" || source === "ad" ? "pollinations" : "openai";
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
 * Grants REWARDED_AD_CREDITS to the FREE adCredits bucket and increments
 * rewardedAdsToday.
 * Returns an error string if the daily cap is already reached, otherwise null.
 *
 * Runs in a transaction so concurrent ad completions don't double-grant.
 *
 * @param dedupKey  Optional idempotency key (e.g. an AdMob SSV transaction_id).
 *                  When supplied, the grant is recorded in
 *                  `processedAdRewards/{dedupKey}` and a repeat call with the
 *                  same key is a no-op — so SSV retries never double-credit.
 */
export async function grantRewardedAdCreditsForUser(
  uid: string,
  offsetMinutes = 0,
  dedupKey?: string
): Promise<{error: string | null; newBalance: number; rewardedAdsToday: number}> {
  let newBalance = 0;
  let newAdsToday = 0;
  let error: string | null = null;

  await db.runTransaction(async (tx) => {
    const ref = userRef(uid);
    const ledgerRef = dedupKey
      ? db.collection(Collections.processedAdRewards).doc(dedupKey)
      : null;

    // All reads must precede all writes in a Firestore transaction.
    const ledgerSnap = ledgerRef ? await tx.get(ledgerRef) : null;
    const snap = await tx.get(ref);
    if (!snap.exists) throw new Error(`User doc ${uid} not found`);

    const user = normalizeUser(snap.data() as UserDoc, offsetMinutes);

    // Idempotency: this reward was already granted (SSV retry / replay).
    if (ledgerSnap?.exists) {
      newBalance = computeQuota(user).totalAvailableCredits;
      newAdsToday = user.rewardedAdsToday ?? 0;
      return;
    }

    const adsToday = user.rewardedAdsToday ?? 0;
    if (adsToday >= MAX_REWARDED_ADS_PER_DAY) {
      error = "daily_ad_limit_reached";
      newBalance = computeQuota(user).totalAvailableCredits;
      newAdsToday = adsToday;
      return; // no changes
    }

    const updated = {
      // Rewarded-ad credits go to the FREE "ad" bucket — NOT extraCredits —
      // so they're always fulfilled by the free provider, never OpenAI.
      adCredits: (user.adCredits ?? 0) + REWARDED_AD_CREDITS,
      rewardedAdsToday: adsToday + 1,
      rewardedAdsDate: user.rewardedAdsDate,
      // Persist any lazy daily reset that happened inside normalizeUser
      dailyCreditsDate: user.dailyCreditsDate,
      dailyCreditsAvailable: user.dailyCreditsAvailable,
      lastDailyGrantAt: user.lastDailyGrantAt ?? null,
      updatedAt: Timestamp.now(),
    };
    tx.update(ref, updated);
    if (ledgerRef) {
      tx.set(ledgerRef, {uid, grantedAt: Timestamp.now()});
    }

    newBalance =
      (user.dailyCreditsAvailable ?? 0) +
      updated.adCredits +
      Math.max(0, user.monthlySketchLimit - user.usedSketchesThisMonth) +
      (user.extraCredits ?? 0);
    newAdsToday = updated.rewardedAdsToday;
  });

  return {error, newBalance, rewardedAdsToday: newAdsToday};
}

/**
 * Sets a user's subscription entitlement from an authoritative Play signal
 * (RTDN handler). When `active` is false the user reverts to the free plan.
 */
export async function setSubscriptionState(
  uid: string,
  active: boolean,
  expiresAt: Timestamp | null
): Promise<void> {
  await userRef(uid).update({
    plan: active ? "pro" : "free",
    subscriptionActive: active,
    monthlySketchLimit: active ? PRO_MONTHLY_LIMIT : 0,
    subscriptionExpiresAt: expiresAt,
    ...(active && expiresAt ? {monthlyResetAt: expiresAt} : {}),
    updatedAt: Timestamp.now(),
  });
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

    let dailyCreditsAvailable = user.dailyCreditsAvailable ?? 0;
    let usedSketchesThisMonth = user.usedSketchesThisMonth ?? 0;
    let extraCredits = user.extraCredits ?? 0;
    let adCredits = user.adCredits ?? 0;
    if (source === "daily") dailyCreditsAvailable = Math.max(0, dailyCreditsAvailable - 1);
    else if (source === "ad") adCredits = Math.max(0, adCredits - 1);
    else if (source === "monthly") usedSketchesThisMonth += 1;
    else if (source === "extra") extraCredits = Math.max(0, extraCredits - 1);
    else {
      // No credit at deduction time — a concurrent request spent the last one.
      // Abort WITHOUT writing the sketch so the user can't get a free
      // generation by racing requests. The transaction rolls back cleanly.
      // eslint-disable-next-line no-console
      console.warn(`commitSketch: ${uid} had no credit at deduction time`);
      throw new NoCreditError();
    }

    tx.update(ref, {
      dailyCreditsAvailable,
      dailyCreditsDate: user.dailyCreditsDate,
      lastDailyGrantAt: user.lastDailyGrantAt ?? null,
      rewardedAdsDate: user.rewardedAdsDate,
      rewardedAdsToday: user.rewardedAdsToday,
      usedSketchesThisMonth,
      extraCredits,
      adCredits,
      monthlyResetAt: user.monthlyResetAt,
      updatedAt: Timestamp.now(),
    });
    tx.set(db.collection(Collections.sketches).doc(sketch.sketchId), {
      ...sketch,
      createdAt: Timestamp.now(),
    });
  });
}
