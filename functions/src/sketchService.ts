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
 * Wraps the child's idea in a strict 3D-STYLE coloring-page template.
 *
 * The output is still clean black-and-white line art the child colors in, but
 * the subject is drawn to look three-dimensional: rounded volumetric forms,
 * gentle perspective/depth, and light interior contour lines that suggest
 * roundness — like a 3D cartoon character turned into a printable coloring page.
 *
 * The template does two jobs at once:
 *  1. Restate the safety constraints (defence in depth — the image model has
 *     its own moderation, but reinforcing here cuts edge cases).
 *  2. Lock the output to a 3D-LOOK line drawing that is still COLORABLE: pure
 *     black outlines on white with large open regions and NO solid shading /
 *     gradients / color (image models love to add those unless told repeatedly).
 *
 * Phrasing tricks that materially improve output quality:
 *   • "3D-style cartoon ... volumetric rounded forms with depth and perspective"
 *     — pushes the model toward dimensional shapes instead of flat icons
 *   • "thick clean black outlines on a pure white background" — keeps it line art
 *   • "light, thin interior contour lines only" — adds 3D form without filling
 *     the regions the child needs to color
 *   • Negative constraints repeated — image models weight repeats more strongly
 */
export function buildColoringPrompt(childPrompt: string): string {
  // Pushes hard toward a 3D LOOK while staying colorable. The tension: real 3D
  // needs shading, but a coloring page must stay blank inside — so we lean on
  // the depth cues that DON'T fill the regions (perspective, three-quarter
  // angle, volumetric rounded forms, interior contour lines, a ground shadow).
  return `A cute 3D-style cartoon ${childPrompt}, drawn as a coloring page for kids (ages 4-10).
Make it clearly THREE-DIMENSIONAL: a chunky, rounded, VOLUMETRIC character shown from a slightly angled three-quarter view with strong depth and perspective — like a 3D-rendered Pixar-style character turned into a coloring outline.
Use bold clean BLACK outlines on a PURE WHITE background, with thin interior CONTOUR lines along curved edges to show roundness and form, and a simple oval outline shadow on the ground beneath a single centered subject.
Keep big open white areas inside the shapes so a child can still color it in.
Avoid: solid color, heavy gray shading, gradients, tiny details, text, logos, branded characters, real people, and anything scary or unsafe for young children.`;
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
  // Pollinations gated its free tier: anonymous requests are limited to ONE
  // queued request per IP, which is unusable from a shared Cloud Functions IP
  // (you'll see HTTP 402 "Queue full for IP …"). An API key lifts the limit to
  // a per-ACCOUNT quota. Put the key in functions/.env as POLLINATIONS_TOKEN=...
  const token = process.env.POLLINATIONS_TOKEN?.trim();
  const encoded = encodeURIComponent(prompt);

  // Endpoint choice matters:
  //  • With an API key → the NEW unified endpoint gen.pollinations.ai, which
  //    actually honours the key (per-account quota / Pollen credits).
  //  • Without a key → the legacy anonymous endpoint (heavily rate-limited;
  //    usually returns HTTP 402 "Queue full" from a shared server IP).
  // The old image.pollinations.ai endpoint IGNORES the new keys, which is why
  // adding the key there changed nothing.
  let url: string;
  const headers: Record<string, string> = {};
  if (token) {
    url =
      "https://gen.pollinations.ai/image/" +
      encoded +
      `?width=1024&height=1024&model=flux&key=${encodeURIComponent(token)}`;
    headers.Authorization = `Bearer ${token}`;
  } else {
    url =
      "https://image.pollinations.ai/prompt/" +
      encoded +
      "?width=1024&height=1024&nologo=true&model=flux";
  }

  const fetchImage = async (): Promise<Buffer> => {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 60_000);
    try {
      const res = await fetch(url, {signal: controller.signal, headers});
      if (!res.ok) {
        const body = (await res.text().catch(() => "")).slice(0, 300);
        throw new Error(`Pollinations HTTP ${res.status}: ${body}`);
      }
      const contentType = res.headers.get("content-type") ?? "";
      const buf = Buffer.from(await res.arrayBuffer());
      // Pollinations sometimes returns a small HTML/error page with 200 OK —
      // guard against uploading a non-image as a "sketch".
      if (!contentType.startsWith("image/") || buf.length < 1000) {
        throw new Error(
          `Pollinations returned a non-image (type=${contentType}, bytes=${buf.length})`
        );
      }
      return buf;
    } finally {
      clearTimeout(timer);
    }
  };

  // The free queue allows 1 in-flight request, so retry with back-off rather
  // than firing concurrent calls (which would just keep hitting "queue full").
  const maxAttempts = 3;
  let lastErr: unknown = new Error("Pollinations: no attempt ran");
  for (let i = 0; i < maxAttempts; i++) {
    try {
      return await fetchImage();
    } catch (e) {
      lastErr = e;
      // eslint-disable-next-line no-console
      console.warn(`Pollinations attempt ${i + 1}/${maxAttempts} failed:`, e);
      if (i < maxAttempts - 1) {
        await new Promise((resolve) => setTimeout(resolve, 2500));
      }
    }
  }
  throw lastErr instanceof Error ? lastErr : new Error(String(lastErr));
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
