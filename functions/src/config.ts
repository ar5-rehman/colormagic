/**
 * Central configuration: secrets, model IDs, plan numbers, product IDs.
 * Everything tunable lives here so the function bodies stay declarative.
 */
import {defineSecret} from "firebase-functions/params";

/**
 * OpenAI API key — stored as a Functions secret, never in source/env files.
 * Set it once with:
 *   firebase functions:secrets:set OPENAI_API_KEY
 * Any function that calls OpenAI must list this in its `secrets: [...]`.
 */
export const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

/** Deploy region — keep functions, Firestore and Storage in the same region. */
export const REGION = "us-central1";

/**
 * App Check enforcement on the callable functions — ON for release, so only
 * the genuine Play-installed app can call the backend (and spend AI credits).
 *
 * IMPORTANT: this is enforced by the function itself — the App Check console's
 * "Unenforced" toggle does NOT override it. Before deploying with this on:
 *  • Firebase Console → App Check → Apps → Android app → Play Integrity, with
 *    the Play App Signing SHA-256 (Play Console → App integrity).
 *  • Only builds installed FROM Google Play pass Play Integrity. Sideloaded /
 *    Android Studio builds fail with "App attestation failed": register their
 *    debug token (printed in Logcat) under App Check → Manage debug tokens, or
 *    temporarily set this to false for local development.
 */
export const ENFORCE_APP_CHECK = true;

/**
 * Cost guard: the most generateSketch instances that can run at once. Each one
 * handles a single request (512MiB → concurrency 1) and calls the paid image
 * API, so this caps both parallel AI spend and runaway scaling from abuse or a
 * bug. Requests beyond it queue briefly, then fail with "resource-exhausted".
 * Raise it as real traffic grows (Cloud Console → Cloud Run shows usage).
 */
export const GENERATE_MAX_INSTANCES = 20;

/**
 * "Delete account" is a soft delete: the account is kept (inactive) for this
 * many days so the parent can restore it, then permanently erased by the daily
 * purgeDeletedAccounts job. Must match the Privacy Policy.
 */
export const ACCOUNT_RESTORE_DAYS = 60;

/** Cap for every other (cheap) callable/HTTP function. */
export const DEFAULT_MAX_INSTANCES = 10;

/** Must match the Android app's applicationId (release variant, no suffix). */
export const ANDROID_PACKAGE_NAME = "com.colormagic.kids";

// ── Plan economics ─────────────────────────────────────────────────────
export const FREE_SKETCHES_TOTAL = 3;     // legacy lifetime free sketches (kept for migration)
export const PRO_MONTHLY_LIMIT = 50;      // sketches/month on the Pro plan
export const EXTRA_PACK_CREDITS = 20;     // credits granted by one extra pack
export const MONTHLY_PERIOD_DAYS = 30;    // approximate billing-cycle length

// ── Daily credit economy (hybrid free / rewarded-ad / premium model) ───
export const FREE_DAILY_CREDITS = 0;          // no ongoing free daily credit (see welcome credits)
// Welcome credits: a NEW user (only the first account ever made on a phone)
// gets WELCOME_CREDITS_PER_DAY on each of their first WELCOME_DAYS days. They
// go into the non-expiring free bucket (adCredits), so unused ones carry over —
// and move with the account when the parent signs in with Google. After that,
// credits come from rewarded ads or purchases only.
export const WELCOME_DAYS = 3;
export const WELCOME_CREDITS_PER_DAY = 1;
export const PREMIUM_DAILY_CREDITS = 30;      // premium-tier daily grant
export const REWARDED_AD_CREDITS = 2;         // credits per completed rewarded ad
export const MAX_REWARDED_ADS_PER_DAY = 3;    // cap per calendar day

// ── Credit costs per action ────────────────────────────────────────────
// Only generation costs credits. Saving/exporting artwork to the phone
// gallery is ALWAYS free for every user (free or premium) — there is no
// HD-export or watermark charge.
export const COST_GENERATE_COLORING_PAGE = 1;
export const COST_PREMIUM_STYLE = 2;

// ── Image generation strategy ──────────────────────────────────────────
// Per-bucket provider (see credits.providerForSource):
//   Daily + Ad credits  → Cloudflare Workers AI (FLUX, free tier)
//   Pro + Extra packs   → OpenAI gpt-image-1 (paid for by the user's purchase)
// Rewarded-ad credits are deliberately routed to the FREE provider so that
// honouring an ad reward never costs us a paid OpenAI call.
//
// Kill switch — set to true while the OpenAI account has no billing. While
// on, ALL credits use Cloudflare regardless of plan (so paid users still get
// *something* instead of an error). Flip to false the moment billing is added
// to the OpenAI account and no other code change is needed; paid users will
// immediately start getting OpenAI images on their next sketch.
export const FORCE_FREE_PROVIDER = true;

/** When false, the OpenAI Moderations API call is skipped. Leave false while
 *  the OpenAI account has no billing (the endpoint will 401 even though it's
 *  free) and turn back to true once billing is set up. Keyword blocklist
 *  (promptSafety.keywordCheck) runs either way. */
export const USE_OPENAI_MODERATION = false;

// ── OpenAI image models (only used when IMAGE_PROVIDER === "openai") ───
// NOTE: verify these model IDs against the CURRENT OpenAI Image API before
// going live — the published names change. The app deliberately never shows
// these strings to users (they see "Free Sketch" / "Premium Sketch").
export const MODEL_FREE = "dall-e-2";     // free-sketch credits
export const MODEL_PREMIUM = "gpt-image-1";       // monthly + extra credits
export const IMAGE_SIZE = "1024x1024";
export const IMAGE_QUALITY = "low";

// ── Google Play product IDs (must match Play Console exactly) ──────────
export const PRODUCT_MONTHLY_PRO = "monthly_pro";       // subscription
export const PRODUCT_EXTRA_20 = "extra_20_sketches";    // one-time in-app

// ── AdMob rewarded Server-Side Verification (SSV) ──────────────────────
// When SSV is configured in the AdMob console (Rewarded ad unit → SSV →
// callback URL = the admobSsvCallback function URL), AdMob calls our backend
// DIRECTLY on a verified reward. That is the authoritative grant and cannot be
// forged by the client. Keep in sync with USE_ADMOB_SSV in the Android
// CreditConfig. Google's TEST ad units never send SSV callbacks — set both to
// false only while testing with test ad IDs.
// OFF: AdMob wasn't delivering SSV callbacks for the live ad unit, so rewards
// never arrived. The app now grants via the grantRewardedAdCredits callable
// right after AdMob's onUserEarnedReward — protected by App Check (genuine
// Play app only) and the server-side MAX_REWARDED_ADS_PER_DAY cap. While off,
// admobSsvCallback grants nothing (no double credits). Turn both flags back on
// once SSV callbacks are confirmed arriving in the logs.
export const USE_ADMOB_SSV = false;
/** Google's public keys used to verify rewarded SSV callback signatures. */
export const ADMOB_VERIFIER_KEYS_URL =
  "https://www.gstatic.com/admob/reward/verifier-keys.json";
/** Reject SSV callbacks whose timestamp is older than this (replay defense). */
export const ADMOB_SSV_MAX_AGE_MS = 60 * 60 * 1000; // 1 hour

// ── Google Play Real-time Developer Notifications (RTDN) ────────────────
// Pub/Sub topic that Play Console publishes subscription lifecycle events to
// (renew / cancel / refund / expire). The playRtdnHandler function subscribes
// to it and revokes "pro" when a subscription is no longer valid. Create the
// topic and set it in Play Console → Monetization setup → Real-time
// developer notifications, then redeploy.
export const PLAY_RTDN_TOPIC = "play-rtdn";

/** Hard cap on prompt length — rejects obviously abusive input early. */
export const MAX_PROMPT_LENGTH = 200;
