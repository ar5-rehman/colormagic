/**
 * Callable: generateSketch
 *
 * Android invokes this via the Firebase Functions SDK, which auto-attaches
 * the user's Auth token AND an App Check token — so the OpenAI key never
 * leaves the backend and only the genuine app build can call in.
 *
 * Order of operations (a credit is spent only if EVERYTHING succeeds):
 *   1. Authenticate the caller.
 *   2. Validate + safety-check the prompt.
 *   3. Pre-check credits; pick the model from the credit bucket.
 *   4. Generate the image (Cloudflare Workers AI) → upload to Storage.
 *   5. Atomically deduct 1 credit + write the sketch document.
 */
import {randomUUID} from "crypto";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {
  ENFORCE_APP_CHECK,
  IMAGE_QUALITY,
  MAX_PROMPT_LENGTH,
  MODEL_FREE,
  REGION,
} from "./config";
import {
  type ImageProvider,
  clampOffsetMinutes,
  commitSketchAndDeduct,
  ensureUserDoc,
  modelForSource,
  NoCreditError,
  pickCreditSource,
  providerForSource,
} from "./credits";
import {UNSAFE_PROMPT_MESSAGE, checkPromptSafety} from "./promptSafety";
import {
  buildColoringPrompt,
  generateColoringImage,
  uploadSketch,
} from "./sketchService";
import {GenerateSketchRequest, GenerateSketchResponse} from "./types";

export const generateSketch = onCall(
  {
    region: REGION,
    enforceAppCheck: ENFORCE_APP_CHECK,
    timeoutSeconds: 120,
    memory: "512MiB",
  },
  async (request): Promise<GenerateSketchResponse> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign-in required.");
    }

    const reqData = (request.data ?? {}) as GenerateSketchRequest &
      {utcOffsetMinutes?: number};
    const {prompt: rawPrompt, isChallenge} = reqData;
    const offset = clampOffsetMinutes(reqData.utcOffsetMinutes);
    const prompt = (rawPrompt ?? "").trim();
    const challenge = isChallenge === true;
    if (!prompt) {
      throw new HttpsError("invalid-argument", "Please describe a picture first.");
    }
    if (prompt.length > MAX_PROMPT_LENGTH) {
      throw new HttpsError("invalid-argument", "That idea is a bit too long.");
    }

    // 1. Safety — keyword blocklist (kid-specific risks: real people, brands,
    //    scary/adult terms). OpenAI moderation is disabled for now; the prompt
    //    template + blocklist are the safety net.
    const safety = checkPromptSafety(prompt);
    if (!safety.safe) {
      console.warn(`Unsafe prompt from ${uid}: ${safety.reason}`);
      if (safety.reason === "moderation:error") {
        throw new HttpsError(
          "internal",
          "We couldn't check that idea right now. Please try again."
        );
      }
      throw new HttpsError("invalid-argument", UNSAFE_PROMPT_MESSAGE);
    }

    // 2. Credit pre-check. Fast-fail before spending an OpenAI call.
    //    Challenges are free — skip credit checks entirely.
    const user = await ensureUserDoc(uid, offset);
    let source: ReturnType<typeof pickCreditSource> = null;
    let model: string;
    let provider: ImageProvider;
    if (challenge) {
      model = MODEL_FREE;
      provider = "cloudflare";
    } else {
      source = pickCreditSource(user);
      if (!source) {
        throw new HttpsError(
          "resource-exhausted",
          "No sketch credits left. Ask a grown-up to unlock more."
        );
      }
      model = modelForSource(source);
      provider = providerForSource(source);
    }

    // 3. Generate the line art. Failure here spends NO credit.
    let png: Buffer;
    try {
      png = await generateColoringImage(provider, buildColoringPrompt(prompt));
    } catch (err) {
      // Full reason is logged server-side for diagnosis; the child only ever
      // sees the friendly message below (rendered on the "Hmm, that didn't
      // work" screen with a Try Again button).
      console.error(`Image generation failed (provider=${provider}, model=${model})`, err);
      throw new HttpsError(
        "internal",
        "Our magic crayons need a quick rest. Please try again in a moment!"
      );
    }

    // 4. Upload. Failure here also spends NO credit.
    const sketchId = randomUUID();
    let uploaded;
    try {
      uploaded = await uploadSketch(uid, sketchId, png);
    } catch (err) {
      console.error("Storage upload failed", err);
      throw new HttpsError("internal", "We couldn't save that sketch. Please try again.");
    }

    // 5. Only now: deduct 1 credit AND record the sketch, atomically.
    //    Challenges skip credit deduction — they're free.
    let streakAdvancedToday = false;
    if (!challenge) {
      try {
        const result = await commitSketchAndDeduct(uid, {
          sketchId,
          userId: uid,
          prompt,
          model,
          quality: IMAGE_QUALITY,
          imageUrl: uploaded.imageUrl,
          storagePath: uploaded.storagePath,
        }, offset);
        streakAdvancedToday = result.streakAdvancedToday;
      } catch (err) {
        if (err instanceof NoCreditError) {
          throw new HttpsError(
            "resource-exhausted",
            "No sketch credits left. Ask a grown-up to unlock more."
          );
        }
        throw err;
      }
    }

    return {success: true, sketchId, imageUrl: uploaded.imageUrl, streakAdvancedToday};
  }
);
