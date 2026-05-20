/**
 * Sketch generation pipeline: build a safe prompt → call OpenAI → upload the
 * PNG to Firebase Storage. Pure of credit logic — the caller wires that in.
 */
import {randomUUID} from "crypto";
import type OpenAI from "openai";
import {storage} from "./firebase";
import {IMAGE_QUALITY, IMAGE_SIZE} from "./config";

/**
 * Wraps the child's idea in a strict coloring-page template. The template
 * forces black-and-white line art and re-states the safety constraints so
 * the image model has them even though the prompt already passed our gate.
 */
export function buildColoringPrompt(childPrompt: string): string {
  return `Create a child-friendly black-and-white coloring book page.

Subject:
${childPrompt}

Style:
- simple clean line art
- thick black outlines
- white background
- large open spaces for coloring
- cute, friendly, cheerful
- centered composition
- suitable for children ages 4 to 10

Important:
- no color
- no gray shading
- no gradients
- no tiny details
- no text
- no watermark
- no logos
- no copyrighted characters
- no real person likeness
- no scary, violent, adult, romantic, or unsafe content`;
}

/**
 * Calls the OpenAI Image API and returns the PNG bytes.
 *
 * The request is built as a plain object and passed loosely typed: the model
 * IDs in config.ts are project-specific and may not match the SDK's compiled
 * union, so we avoid a hard type dependency on them.
 */
export async function generateColoringImage(
  openai: OpenAI,
  model: string,
  coloringPrompt: string
): Promise<Buffer> {
  // `quality` is only valid on gpt-image-1 / dall-e-3. dall-e-2 rejects the
  // request outright if we send it. Build the param object accordingly.
  const params: Record<string, unknown> = {
    model,
    prompt: coloringPrompt,
    size: IMAGE_SIZE,
    n: 1,
  };
  if (model.startsWith("gpt-image") || model === "dall-e-3") {
    params.quality = IMAGE_QUALITY;
  }
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const result = await openai.images.generate(params as any);

  const first = result.data?.[0];
  if (!first) throw new Error("OpenAI returned no image data");

  // gpt-image models return base64; dall-e models may return a URL.
  if (first.b64_json) {
    return Buffer.from(first.b64_json, "base64");
  }
  if (first.url) {
    const res = await fetch(first.url);
    if (!res.ok) throw new Error(`Image fetch failed: HTTP ${res.status}`);
    return Buffer.from(await res.arrayBuffer());
  }
  throw new Error("OpenAI image had neither b64_json nor url");
}

export interface UploadedSketch {
  storagePath: string;
  imageUrl: string;
}

/**
 * Uploads the sketch PNG to `sketches/{uid}/{sketchId}.png` and returns a
 * stable, tokened Firebase download URL the Android app can load directly
 * (e.g. with Coil). The download token is its own access grant, so the URL
 * keeps working without making the whole bucket public.
 */
export async function uploadSketch(
  uid: string,
  sketchId: string,
  png: Buffer
): Promise<UploadedSketch> {
  const storagePath = `sketches/${uid}/${sketchId}.png`;
  const bucket = storage.bucket();
  const file = bucket.file(storagePath);
  const downloadToken = randomUUID();

  await file.save(png, {
    contentType: "image/png",
    resumable: false,
    metadata: {
      contentType: "image/png",
      metadata: {firebaseStorageDownloadTokens: downloadToken},
    },
  });

  const imageUrl =
    `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/` +
    `${encodeURIComponent(storagePath)}?alt=media&token=${downloadToken}`;

  return {storagePath, imageUrl};
}
