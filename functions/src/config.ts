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

/** Must match the Android app's applicationId (release variant, no suffix). */
export const ANDROID_PACKAGE_NAME = "com.colormagic.kids";

// ── Plan economics ─────────────────────────────────────────────────────
export const FREE_SKETCHES_TOTAL = 3;     // legacy lifetime free sketches (kept for migration)
export const PRO_MONTHLY_LIMIT = 50;      // sketches/month on the Pro plan
export const EXTRA_PACK_CREDITS = 20;     // credits granted by one extra pack
export const MONTHLY_PERIOD_DAYS = 30;    // approximate billing-cycle length

// ── Daily credit economy (hybrid free / rewarded-ad / premium model) ───
export const FREE_DAILY_CREDITS = 1;          // free-tier daily grant
export const PREMIUM_DAILY_CREDITS = 30;      // premium-tier daily grant
export const REWARDED_AD_CREDITS = 3;         // credits per completed rewarded ad
export const MAX_REWARDED_ADS_PER_DAY = 5;    // cap per calendar day

// ── Credit costs per action ────────────────────────────────────────────
// Only generation costs credits. Saving/exporting artwork to the phone
// gallery is ALWAYS free for every user (free or premium) — there is no
// HD-export or watermark charge.
export const COST_GENERATE_COLORING_PAGE = 1;
export const COST_PREMIUM_STYLE = 2;

// ── Image generation strategy ──────────────────────────────────────────
// Per-bucket provider (see credits.providerForSource):
//   Daily + Ad credits  → Pollinations (free public Stable Diffusion endpoint)
//   Pro + Extra packs   → OpenAI gpt-image-1 (paid for by the user's purchase)
// Rewarded-ad credits are deliberately routed to the FREE provider so that
// honouring an ad reward never costs us a paid OpenAI call.
//
// Kill switch — set to true while the OpenAI account has no billing. While
// on, ALL credits use Pollinations regardless of plan (so paid users still
// get *something* instead of an error). Flip to false the moment billing is
// added to the OpenAI account and no other code change is needed; paid
// users will immediately start getting OpenAI images on their next sketch.
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
// forged by the client. Until you configure it, leave this false so the
// client-callable grant path keeps working.
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
