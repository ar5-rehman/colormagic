/**
 * Sketch generation pipeline: build a safe prompt → generate the image
 * (Cloudflare Workers AI) → upload the PNG to Firebase Storage. Pure of credit
 * logic — the caller wires that in.
 *
 * NOTE: OpenAI is currently DISABLED — Cloudflare handles ALL generation (free
 * + premium). To re-enable OpenAI later, restore the OpenAI branch here and the
 * provider mapping in credits.providerForSource.
 */
import {randomUUID} from "crypto";
import {storage} from "./firebase";
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
 * Generates the coloring-page image and returns the PNG/JPEG bytes.
 *
 * OpenAI is disabled for now, so the only active provider is Cloudflare. The
 * [provider] argument is kept so re-enabling OpenAI later is a one-spot change.
 */
export async function generateColoringImage(
  provider: ImageProvider,
  coloringPrompt: string
): Promise<Buffer> {
  if (provider !== "cloudflare") {
    throw new Error(
      `Image provider "${provider}" is disabled; only Cloudflare is active.`
    );
  }
  return generateWithCloudflare(coloringPrompt);
}

/**
 * Free image-generation backend. Calls Cloudflare Workers AI (FLUX schnell).
 *
 * Why Cloudflare: a genuine free tier (~10k neurons/day) that works from a
 * server with an API token, no watermark, reliable infra — unlike Pollinations
 * whose free tier became unusable from shared server IPs. The prompt template
 * in buildColoringPrompt() keeps the output a colorable 3D-style line drawing.
 *
 * Setup: put these in functions/.env (loaded automatically at deploy):
 *   CF_ACCOUNT_ID=...   (Cloudflare dashboard → Workers & Pages → Account ID)
 *   CF_API_TOKEN=...    (My Profile → API Tokens → "Workers AI" template)
 */
async function generateWithCloudflare(prompt: string): Promise<Buffer> {
  const accountId = process.env.CF_ACCOUNT_ID?.trim();
  const apiToken = process.env.CF_API_TOKEN?.trim();
  if (!accountId || !apiToken) {
    throw new Error(
      "Cloudflare not configured — set CF_ACCOUNT_ID and CF_API_TOKEN in functions/.env"
    );
  }

  // FLUX schnell: fast, free-tier-friendly. Prompt goes in the JSON body, so
  // there are no URL-length limits. `steps` 1-8 (higher = a bit more detail).
  const url =
    `https://api.cloudflare.com/client/v4/accounts/${accountId}` +
    "/ai/run/@cf/black-forest-labs/flux-1-schnell";

  const attempt = async (): Promise<Buffer> => {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 60_000);
    try {
      const res = await fetch(url, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${apiToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({prompt, steps: 6}),
        signal: controller.signal,
      });

      if (!res.ok) {
        const body = (await res.text().catch(() => "")).slice(0, 400);
        throw new Error(`Cloudflare HTTP ${res.status}: ${body}`);
      }

      const contentType = res.headers.get("content-type") ?? "";
      // flux-1-schnell returns JSON: { success, result: { image: "<base64>" } }
      if (contentType.includes("application/json")) {
        const json = (await res.json()) as {
          success?: boolean;
          result?: {image?: string};
          errors?: unknown;
        };
        const b64 = json.result?.image;
        if (!json.success || !b64) {
          throw new Error(
            `Cloudflare returned no image: ${JSON.stringify(json.errors ?? json).slice(0, 300)}`
          );
        }
        const buf = Buffer.from(b64, "base64");
        if (buf.length < 1000) {
          throw new Error(`Cloudflare image too small (${buf.length} bytes)`);
        }
        return buf;
      }

      // Some Workers AI models return raw image bytes instead of JSON.
      const buf = Buffer.from(await res.arrayBuffer());
      if (!contentType.startsWith("image/") || buf.length < 1000) {
        throw new Error(
          `Cloudflare returned a non-image (type=${contentType}, bytes=${buf.length})`
        );
      }
      return buf;
    } finally {
      clearTimeout(timer);
    }
  };

  try {
    return await attempt();
  } catch (first) {
    // eslint-disable-next-line no-console
    console.warn("Cloudflare attempt 1 failed, retrying once:", first);
    return await attempt();
  }
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
