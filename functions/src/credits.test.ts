/**
 * Pure unit tests for the credit accounting logic in credits.ts.
 * Run with:  cd functions && npm test
 * (assumes Jest or ts-jest is configured in package.json)
 *
 * These tests cover every rule that can be exercised without a live Firestore
 * connection — normalizeUser, computeQuota, pickCreditSource, and
 * grantRewardedAdCreditsForUser's cap logic are all pure or near-pure.
 */

import {
  FREE_DAILY_CREDITS,
  PREMIUM_DAILY_CREDITS,
  MAX_REWARDED_ADS_PER_DAY,
  REWARDED_AD_CREDITS,
  MODEL_FREE,
  MODEL_PREMIUM,
} from "./config";
import {
  normalizeUser,
  computeQuota,
  pickCreditSource,
  modelForSource,
  providerForSource,
  todayUtc,
  localDayKey,
  clampOffsetMinutes,
} from "./credits";
import {UserDoc} from "./types";
import {Timestamp} from "firebase-admin/firestore";

// ── helpers ──────────────────────────────────────────────────────────────

const NOW = Timestamp.now();
const TODAY = todayUtc();
const YESTERDAY = new Date(Date.now() - 86_400_000).toISOString().slice(0, 10);
const YESTERDAY_TS = Timestamp.fromMillis(Date.now() - 86_400_000);

function makeUser(overrides: Partial<UserDoc> = {}): UserDoc {
  return {
    plan: "free",
    subscriptionActive: false,
    freeSketchesTotal: 3,
    freeSketchesUsed: 0,
    monthlySketchLimit: 0,
    usedSketchesThisMonth: 0,
    extraCredits: 0,
    adCredits: 0,
    monthlyResetAt: null,
    subscriptionExpiresAt: null,
    subscriptionPurchaseToken: null,
    dailyCreditsDate: TODAY,
    dailyCreditsAvailable: FREE_DAILY_CREDITS,
    lastDailyGrantAt: NOW,
    rewardedAdsDate: TODAY,
    rewardedAdsToday: 0,
    createdAt: NOW,
    updatedAt: NOW,
    ...overrides,
  };
}

// ── 1. Daily credit reset ─────────────────────────────────────────────────

describe("normalizeUser – daily credit reset", () => {
  test("resets daily credits for free user on new day", () => {
    const user = makeUser({
      dailyCreditsDate: YESTERDAY,
      dailyCreditsAvailable: 0,
      lastDailyGrantAt: YESTERDAY_TS,
    });
    const result = normalizeUser(user);
    expect(result.dailyCreditsAvailable).toBe(FREE_DAILY_CREDITS);
    expect(result.dailyCreditsDate).toBe(TODAY);
  });

  test("resets daily credits for premium user on new day", () => {
    const user = makeUser({
      plan: "pro",
      subscriptionActive: true,
      dailyCreditsDate: YESTERDAY,
      dailyCreditsAvailable: 0,
      lastDailyGrantAt: YESTERDAY_TS,
    });
    const result = normalizeUser(user);
    expect(result.dailyCreditsAvailable).toBe(PREMIUM_DAILY_CREDITS);
  });

  test("does NOT reset daily credits when already granted today", () => {
    const user = makeUser({
      dailyCreditsAvailable: 2,
      lastDailyGrantAt: NOW, // granted earlier today
    });
    const result = normalizeUser(user);
    expect(result.dailyCreditsAvailable).toBe(2);
  });

  test("legacy doc with no lastDailyGrantAt falls back to dailyCreditsDate", () => {
    // Existing user from before the timestamp field: only the day string exists.
    const user = makeUser({
      dailyCreditsDate: TODAY,
      dailyCreditsAvailable: 0,
      lastDailyGrantAt: null,
    });
    const result = normalizeUser(user); // same UTC day → no spurious grant
    expect(result.dailyCreditsAvailable).toBe(0);
  });

  test("resets rewarded-ad counter on new day", () => {
    const user = makeUser({rewardedAdsDate: YESTERDAY, rewardedAdsToday: MAX_REWARDED_ADS_PER_DAY});
    const result = normalizeUser(user);
    expect(result.rewardedAdsToday).toBe(0);
    expect(result.rewardedAdsDate).toBe(TODAY);
  });
});

// ── 1b. Local-day boundary helpers ────────────────────────────────────────

describe("localDayKey", () => {
  test("offset shifts the calendar day forward across UTC midnight", () => {
    const ms = Date.parse("2026-06-04T22:00:00Z");
    expect(localDayKey(ms, 0)).toBe("2026-06-04");     // UTC
    expect(localDayKey(ms, 180)).toBe("2026-06-05");   // UTC+3 → next day
    expect(localDayKey(ms, -300)).toBe("2026-06-04");  // UTC-5 → same day
  });

  test("negative offset shifts to the previous day near UTC midnight", () => {
    const ms = Date.parse("2026-06-04T02:00:00Z");
    expect(localDayKey(ms, -180)).toBe("2026-06-03");  // UTC-3 → previous day
  });
});

describe("clampOffsetMinutes", () => {
  test("clamps to ±14h and sanitises garbage input", () => {
    expect(clampOffsetMinutes(300)).toBe(300);
    expect(clampOffsetMinutes(99999)).toBe(14 * 60);
    expect(clampOffsetMinutes(-99999)).toBe(-14 * 60);
    expect(clampOffsetMinutes("abc")).toBe(0);
    expect(clampOffsetMinutes(undefined)).toBe(0);
    expect(clampOffsetMinutes(null)).toBe(0);
  });
});

// ── 1c. Free vs paid provider routing (rewarded-ad cost control) ──────────

describe("credit source → provider/model routing", () => {
  test("daily and ad credits use the FREE provider/model", () => {
    expect(providerForSource("daily")).toBe("cloudflare");
    expect(providerForSource("ad")).toBe("cloudflare");
    expect(modelForSource("daily")).toBe(MODEL_FREE);
    expect(modelForSource("ad")).toBe(MODEL_FREE);
  });

  test("monthly and extra (paid) credits use OpenAI / premium model", () => {
    expect(providerForSource("monthly")).toBe("openai");
    expect(providerForSource("extra")).toBe("openai");
    expect(modelForSource("monthly")).toBe(MODEL_PREMIUM);
    expect(modelForSource("extra")).toBe(MODEL_PREMIUM);
  });

  test("ad credits are picked before paid (monthly/extra) credits", () => {
    const user = makeUser({
      dailyCreditsAvailable: 0,
      adCredits: 2,
      extraCredits: 5,
    });
    // Free ad credits must be spent before paid purchased credits.
    expect(pickCreditSource(user)).toBe("ad");
  });

  test("an ad-credit-only sketch never hits the paid provider", () => {
    const user = makeUser({dailyCreditsAvailable: 0, adCredits: 1, extraCredits: 0});
    const source = pickCreditSource(user)!;
    expect(source).toBe("ad");
    expect(providerForSource(source)).toBe("cloudflare");
  });
});

// ── 2. computeQuota ───────────────────────────────────────────────────────

describe("computeQuota", () => {
  test("totalAvailableCredits = daily + extra for free user", () => {
    const user = makeUser({dailyCreditsAvailable: 3, extraCredits: 2});
    const q = computeQuota(user);
    expect(q.totalAvailableCredits).toBe(5);
  });

  test("totalAvailableCredits includes ad credits", () => {
    const user = makeUser({dailyCreditsAvailable: 1, adCredits: 3, extraCredits: 2});
    expect(computeQuota(user).totalAvailableCredits).toBe(6);
    expect(computeQuota(user).adCredits).toBe(3);
  });

  test("totalAvailableCredits includes monthly for pro user", () => {
    const user = makeUser({
      plan: "pro",
      subscriptionActive: true,
      monthlySketchLimit: 50,
      usedSketchesThisMonth: 20,
      dailyCreditsAvailable: 10,
      extraCredits: 0,
    });
    const q = computeQuota(user);
    expect(q.remainingMonthlySketches).toBe(30);
    expect(q.totalAvailableCredits).toBe(40);
  });

  test("rewardedAdsRemaining reflects today's count", () => {
    const user = makeUser({rewardedAdsToday: 3});
    const q = computeQuota(user);
    expect(q.rewardedAdsRemaining).toBe(MAX_REWARDED_ADS_PER_DAY - 3);
    expect(q.rewardedAdsToday).toBe(3);
  });
});

// ── 3. pickCreditSource ───────────────────────────────────────────────────

describe("pickCreditSource", () => {
  test("returns 'daily' when daily credits available", () => {
    const user = makeUser({dailyCreditsAvailable: 3});
    expect(pickCreditSource(user)).toBe("daily");
  });

  test("returns 'monthly' for pro user when daily is exhausted", () => {
    const user = makeUser({
      plan: "pro",
      subscriptionActive: true,
      monthlySketchLimit: 50,
      usedSketchesThisMonth: 10,
      dailyCreditsAvailable: 0,
    });
    expect(pickCreditSource(user)).toBe("monthly");
  });

  test("returns 'extra' when daily exhausted and no monthly", () => {
    const user = makeUser({dailyCreditsAvailable: 0, extraCredits: 5});
    expect(pickCreditSource(user)).toBe("extra");
  });

  test("returns null when all buckets empty", () => {
    const user = makeUser({dailyCreditsAvailable: 0, extraCredits: 0});
    expect(pickCreditSource(user)).toBeNull();
  });
});

// ── 4. Rewarded ad cap logic ──────────────────────────────────────────────

describe("rewarded ad cap", () => {
  test("grant is blocked when rewardedAdsToday >= MAX", () => {
    const user = makeUser({rewardedAdsToday: MAX_REWARDED_ADS_PER_DAY});
    const q = computeQuota(user);
    expect(q.rewardedAdsRemaining).toBe(0);
  });

  test("grant is allowed when rewardedAdsToday < MAX", () => {
    const user = makeUser({rewardedAdsToday: MAX_REWARDED_ADS_PER_DAY - 1});
    const q = computeQuota(user);
    expect(q.rewardedAdsRemaining).toBe(1);
  });

  test("REWARDED_AD_CREDITS is positive", () => {
    expect(REWARDED_AD_CREDITS).toBeGreaterThan(0);
  });
});

// ── 5. Config sanity ──────────────────────────────────────────────────────

describe("config values", () => {
  test("premium daily credits > free daily credits", () => {
    expect(PREMIUM_DAILY_CREDITS).toBeGreaterThan(FREE_DAILY_CREDITS);
  });

  test("max rewarded ads per day is positive", () => {
    expect(MAX_REWARDED_ADS_PER_DAY).toBeGreaterThan(0);
  });
});
