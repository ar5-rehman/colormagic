package com.colormagic.kids.presentation.screens.coloring

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * Real "tap to fill" (paint bucket): finds the whole white area enclosed by
 * the sketch's black lines around the tapped point.
 *
 * Works on the fillable mask (see [computeFillableMask]): opaque pixel = paper
 * the child may color, transparent pixel = line. Returns an ALPHA_8 bitmap at
 * mask resolution (opaque = inside the region) that the canvas tints with the
 * chosen color, or null when the tap landed on a line or on the background
 * outside the drawing (any area that reaches the picture's edge).
 *
 * The region is grown by [GROW_PX] pixels so the fill tucks under the
 * anti-aliased line edges instead of leaving a thin white halo; the mask
 * still clips anything that would land on a line.
 */
suspend fun floodFillRegion(
    mask: ImageBitmap,
    tapX: Float,
    tapY: Float,
    canvasWidth: Int,
    canvasHeight: Int
): ImageBitmap? = withContext(Dispatchers.Default) {
    val bmp = mask.asAndroidBitmap()
    val w = bmp.width
    val h = bmp.height
    if (canvasWidth <= 0 || canvasHeight <= 0) return@withContext null

    val sx = (tapX * w / canvasWidth).toInt()
    val sy = (tapY * h / canvasHeight).toInt()
    if (sx !in 0 until w || sy !in 0 until h) return@withContext null

    val pixels = IntArray(w * h)
    bmp.getPixels(pixels, 0, w, 0, 0, w, h)
    fun paper(i: Int) = (pixels[i] ushr 24) > 127

    val start = sy * w + sx
    if (!paper(start)) return@withContext null

    // Scanline flood fill (iterative, no recursion → no stack overflow).
    val region = BooleanArray(w * h)
    // A pixel can be pushed at most twice (from the row above and below).
    val stack = IntArray(w * h * 2 + 1)
    var top = 0
    stack[top++] = start
    while (top > 0) {
        val i = stack[--top]
        if (region[i]) continue
        val y = i / w
        var left = i
        val rowStart = y * w
        while (left > rowStart && paper(left - 1) && !region[left - 1]) left--
        var right = i
        val rowEnd = rowStart + w - 1
        while (right < rowEnd && paper(right + 1) && !region[right + 1]) right++
        // Touching the picture's edge means this is the background around the
        // drawing (the server always leaves a white border around it), not a
        // shape inside it → a mis-tap outside the sketch; don't fill anything.
        if (y == 0 || y == h - 1 || left == rowStart || right == rowEnd) {
            return@withContext null
        }
        for (p in left..right) {
            region[p] = true
            if (y > 0) {
                val up = p - w
                if (paper(up) && !region[up]) stack[top++] = up
            }
            if (y < h - 1) {
                val down = p + w
                if (paper(down) && !region[down]) stack[top++] = down
            }
        }
    }

    val alpha = ByteArray(w * h)
    for (i in region.indices) if (region[i]) alpha[i] = 0xFF.toByte()
    // Grow by GROW_PX (simple square dilation, two passes: rows then columns).
    val grown = dilate(alpha, w, h, GROW_PX)

    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
    out.copyPixelsFromBuffer(ByteBuffer.wrap(grown))
    out.asImageBitmap()
}

private const val GROW_PX = 2

private fun dilate(src: ByteArray, w: Int, h: Int, r: Int): ByteArray {
    val rows = ByteArray(w * h)
    for (y in 0 until h) {
        val o = y * w
        for (x in 0 until w) {
            if (src[o + x] != 0.toByte()) {
                val a = maxOf(0, x - r)
                val b = minOf(w - 1, x + r)
                for (k in a..b) rows[o + k] = 0xFF.toByte()
            }
        }
    }
    val out = ByteArray(w * h)
    for (y in 0 until h) {
        val o = y * w
        for (x in 0 until w) {
            if (rows[o + x] != 0.toByte()) {
                val a = maxOf(0, y - r)
                val b = minOf(h - 1, y + r)
                for (k in a..b) out[k * w + x] = 0xFF.toByte()
            }
        }
    }
    return out
}
