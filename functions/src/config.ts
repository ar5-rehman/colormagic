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
export const FREE_SKETCHES_TOTAL = 3;     // lifetime free sketches per account
export const PRO_MONTHLY_LIMIT = 50;      // sketches/month on the Pro plan
export const EXTRA_PACK_CREDITS = 20;     // credits granted by one extra pack
export const MONTHLY_PERIOD_DAYS = 30;    // approximate billing-cycle length

// ── OpenAI image models ────────────────────────────────────────────────
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

/** Hard cap on prompt length — rejects obviously abusive input early. */
export const MAX_PROMPT_LENGTH = 200;
