/**
 * Sketch generation pipeline: build a safe prompt → call OpenAI → upload the
 * PNG to Firebase Storage. Pure of credit logic — the caller wires that in.
 */
import {randomUUID} from "crypto";
import type OpenAI from "openai";
import {storage} from "./firebase";
import {IMAGE_QUALITY, IMAGE_SIZE} from "./config";
import type {ImageProvider} from "./credits";

/**
 * Wraps the child's idea in a strict coloring-page template. The template
 * forces black-and-white line art and re-states the safety constraints so
 * the image model has them even though the prompt already passed our gate.
 */
export function buildColoringPrompt(childPrompt: string): string {
  // The prompt has to do two jobs simultaneously:
  //  1. Restate the safety constraints (defence in depth — the image model
  //     has its own moderation, but reinforcing here cuts edge cases).
  //  2. Lock the output style to clean line art with NO color and NO shading.
  //     Image models love to add gradients/colors unless told repeatedly.
  //
  // Phrasing tricks that materially improve output quality:
  //   • "in the style of a printable coloring book for young children" —
  //     activates the model's coloring-book training samples
  //   • "vector-like black outlines on a pure white background" — anchors
  //     it to line-art rather than illustration
  //   • Negative constraints listed twice (style + important) — image models
  //     weight repeated instructions more strongly
  return `Create a printable black-and-white coloring book page for a young child (ages 4 to 10).

Subject:
${childPrompt}

Required style:
- in the style of a printable coloring book for young children
- simple, friendly cartoon characters with rounded shapes
- thick clean BLACK outlines on a PURE WHITE background
- vector-like line art only — no shading, no fill, no color of any kind
- large open empty regions inside every shape so the child can color them in
- cute, happy, smiling, gentle expressions
- a single clearly centred subject
- no realistic textures, no realistic faces, no photo-realism

ABSOLUTELY DO NOT INCLUDE:
- color of any kind (the page must be 100% black and white)
- gray shading, hatching, dots, or gradients
- realistic skin, hair, or fur textures
- tiny details that a small child cannot color
- text, letters, numbers, signatures, watermarks, or logos
- copyrighted, branded, or trademarked characters
- real or recognisable people, celebrities, or public figures
- anything scary, violent, sad, adult, romantic, weapon-related, or otherwise unsafe for a 4-year-old
- backgrounds, scenery, or borders that fill the page — leave open whitespace

Think: a friendly illustration a parent would print from a coloring book for their preschooler.`;
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
  provider: ImageProvider,
  model: string,
  coloringPrompt: string
): Promise<Buffer> {
  // Free path — Pollinations.ai. Returns the PNG bytes directly.
  if (provider === "pollinations") {
    return generateWithPollinations(coloringPrompt);
  }
  // Paid path — OpenAI.

  // `quality` is only valid on gpt-image-1 / dall-e-3. dall-e-2 rejects the
  // request outright if we send it. Build the param object accordingly.
  const params: Record<string, unknown> = {
    model,
    prompt: coloringPrompt,
    size: IMAGE_SIZE,
    n: 1,
  };
  if (model.startsWith("gpt-image")) {
    params.quality = IMAGE_QUALITY;
    // Output-side safety on gpt-image-1:
    //   • moderation:"auto" — runs OpenAI's strictest output filter; any
    //     image that touches sexual/violent/etc. content is refused with an
    //     error, which we surface as a generic "try a different idea" to
    //     the child.
    //   • background:"opaque" — forces a flat opaque (white in our prompt)
    //     background rather than transparent, so the page prints cleanly.
    params.moderation = "auto";
    params.background = "opaque";
  } else if (model === "dall-e-3") {
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

/**
 * Free image-generation backend. Calls Pollinations.ai's public Stable
 * Diffusion endpoint — no API key, no cost. The prompt template in
 * buildColoringPrompt() does the heavy lifting to keep output line-art-only.
 *
 * Tradeoffs vs OpenAI:
 *   • No API key / billing required, $0.00 per image
 *   • Generation time ~10–30s (public service, no SLA)
 *   • Output quality lower than gpt-image-1
 *   • No output moderation — relies entirely on our prompt template + the
 *     keyword blocklist run before this is called
 */
async function generateWithPollinations(prompt: string): Promise<Buffer> {
  const url =
    "https://image.pollinations.ai/prompt/" +
    encodeURIComponent(prompt) +
    "?width=1024&height=1024&nologo=true&model=flux";
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Pollinations request failed: HTTP ${res.status}`);
  }
  return Buffer.from(await res.arrayBuffer());
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
