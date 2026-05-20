package com.colormagic.kids.presentation.screens.coloring

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Pre-processes a line-art sketch into a "fillable mask" — an ImageBitmap where
// each pixel is:
//   • opaque black  → paintable region (light pixel in the source)
//   • transparent   → boundary line   (dark pixel in the source)
//
// At render time, the canvas composites this mask over the kid's strokes with
// BlendMode.DstIn. The result: paint only shows on pixels that are *not* lines.
// The kid can stroke as freely as they like; the lines clip the paint.
//
// [lineThreshold] is the brightness cutoff. Sketches with thin, light grey lines
// may want a higher value (try 180–200); thick black ink works fine at the
// default 128. Backend can ship a per-sketch threshold if line styles vary.
/** Builds the fillable mask from a bundled drawable resource (the bundled
 *  sample sketch). Delegates to [computeFillableMask]. */
suspend fun loadFillableMask(
    context: Context,
    @DrawableRes drawableRes: Int,
    lineThreshold: Int = 128
): ImageBitmap = withContext(Dispatchers.Default) {
    val source: Bitmap = BitmapFactory.decodeResource(context.resources, drawableRes)
        ?: error("Could not decode drawable $drawableRes")
    val mask = computeFillableMask(source, lineThreshold)
    source.recycle()
    mask
}

/**
 * Builds the fillable mask from any already-decoded bitmap — used for the
 * backend-generated sketch downloaded from its image URL.
 *
 * The source bitmap must be a software bitmap (`getPixels` can't read a
 * hardware bitmap) — Coil callers must set `allowHardware(false)`.
 */
suspend fun computeFillableMask(
    source: Bitmap,
    lineThreshold: Int = 128
): ImageBitmap = withContext(Dispatchers.Default) {
    val width = source.width
    val height = source.height
    val sourcePixels = IntArray(width * height)
    source.getPixels(sourcePixels, 0, width, 0, 0, width, height)

    // Build the mask in one pass.
    //   ITU-R 601 luma coefficients give a perceptually accurate brightness,
    //   important when the sketch has coloured ink (still treated as boundary).
    val maskPixels = IntArray(width * height) { i ->
        val argb = sourcePixels[i]
        val a = (argb ushr 24) and 0xFF
        // Transparent source pixel → "outside the page", also a boundary.
        if (a < 32) return@IntArray 0

        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val luma = (r * 299 + g * 587 + b * 114) / 1000
        if (luma >= lineThreshold) 0xFF000000.toInt() else 0
    }

    Bitmap.createBitmap(maskPixels, width, height, Bitmap.Config.ARGB_8888)
        .asImageBitmap()
}
