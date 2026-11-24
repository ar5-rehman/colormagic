/**
 * Turns a generated drawing into clean coloring-book line art that fills the
 * page:
 *
 *  1. grayscale + hard threshold → every pixel pure black or pure white
 *     (removes gray shading, blush, soft edges — "colors" the model sneaks in)
 *  2. median filter → drops tiny specks/dots that would need their own tap
 *  3. crop away the empty white margins, then scale the drawing up so it fills
 *     the square page with a small even border → bigger on screen and easier
 *     for small fingers to color
 *  4. light blur + strict re-threshold → slightly thicker lines, which also
 *     closes hairline gaps so tap-to-fill doesn't leak into the background
 *  5. tiny blur → smooth (anti-aliased) edges instead of pixel staircases
 *
 * Output is a square PNG. Never throws: if anything goes wrong the original
 * image is returned unchanged (a less-clean page beats a failed generation).
 */
import sharp from "sharp";

const PAGE_SIZE = 1024;
/** Empty border kept around the drawing, as a fraction of the page. */
const MARGIN_FRACTION = 0.05;

export async function cleanLineArt(image: Buffer): Promise<Buffer> {
  try {
    const bw = await sharp(image)
      .grayscale()
      .threshold(150)
      .median(3)
      .png()
      .toBuffer();

    const fitted = await fillPage(bw);

    const thick = await sharp(fitted)
      .blur(0.8)
      .threshold(200) // anything not near-white becomes line → thicker lines
      .toBuffer();

    // 5. Anti-alias: a hard threshold leaves jagged "staircase" edges. A tiny
    // blur softens only the edge pixels; line cores stay solid black, so the
    // app's fill mask (luma < 128 = line) sees the same lines as before.
    return await sharp(thick)
      .blur(0.6)
      .png({compressionLevel: 9})
      .toBuffer();
  } catch (err) {
    console.warn("cleanLineArt failed, using original image", err);
    return image;
  }
}

/** Crops white margins and centres the drawing on a square white page. */
async function fillPage(bw: Buffer): Promise<Buffer> {
  let trimmed: Buffer;
  try {
    trimmed = await sharp(bw)
      .trim({background: "#ffffff", threshold: 10})
      .toBuffer();
  } catch {
    return bw; // e.g. an all-white image: nothing to crop
  }
  const margin = Math.round(PAGE_SIZE * MARGIN_FRACTION);
  const inner = PAGE_SIZE - margin * 2;
  return sharp(trimmed)
    .resize(inner, inner, {fit: "contain", background: "#ffffff", kernel: "lanczos3"})
    .flatten({background: "#ffffff"})
    .extend({top: margin, bottom: margin, left: margin, right: margin, background: "#ffffff"})
    .png()
    .toBuffer();
}
