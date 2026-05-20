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
 *   4. Generate the image (OpenAI) → upload to Storage.
 *   5. Atomically deduct 1 credit + write the sketch document.
 */
import {randomUUID} from "crypto";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import OpenAI from "openai";
import {IMAGE_QUALITY, MAX_PROMPT_LENGTH, OPENAI_API_KEY, REGION} from "./config";
import {
  commitSketchAndDeduct,
  ensureUserDoc,
  modelForSource,
  pickCreditSource,
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
    enforceAppCheck: true,
    secrets: [OPENAI_API_KEY],
    timeoutSeconds: 120,
    memory: "512MiB",
  },
  async (request): Promise<GenerateSketchResponse> => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign-in required.");
    }

    const {prompt: rawPrompt} = (request.data ?? {}) as GenerateSketchRequest;
    const prompt = (rawPrompt ?? "").trim();
    if (!prompt) {
      throw new HttpsError("invalid-argument", "Please describe a picture first.");
    }
    if (prompt.length > MAX_PROMPT_LENGTH) {
      throw new HttpsError("invalid-argument", "That idea is a bit too long.");
    }

    const openai = new OpenAI({apiKey: OPENAI_API_KEY.value()});

    // 1. Safety — keyword blocklist + OpenAI moderation. Must clear both.
    const safety = await checkPromptSafety(openai, prompt);
    if (!safety.safe) {
      console.warn(`Unsafe prompt from ${uid}: ${safety.reason}`);
      throw new HttpsError("invalid-argument", UNSAFE_PROMPT_MESSAGE);
    }

    // 2. Credit pre-check. Fast-fail before spending an OpenAI call.
    //    The model is chosen by the bucket the credit will come from.
    const user = await ensureUserDoc(uid);
    const source = pickCreditSource(user);
    if (!source) {
      throw new HttpsError(
        "resource-exhausted",
        "No sketch credits left. Ask a grown-up to unlock more."
      );
    }
    const model = modelForSource(source);

    // 3. Generate the line art. Failure here spends NO credit.
    let png: Buffer;
    try {
      png = await generateColoringImage(openai, model, buildColoringPrompt(prompt));
    } catch (err) {
      console.error("OpenAI generation failed", err);
      throw new HttpsError("internal", "We couldn't draw that sketch. Please try again.");
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
    await commitSketchAndDeduct(uid, {
      sketchId,
      userId: uid,
      prompt,
      model,
      quality: IMAGE_QUALITY,
      imageUrl: uploaded.imageUrl,
      storagePath: uploaded.storagePath,
    });

    return {success: true, sketchId, imageUrl: uploaded.imageUrl};
  }
);
