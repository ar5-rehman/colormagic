package com.colormagic.kids.data.gallery

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.colormagic.kids.domain.model.BrushSize
import com.colormagic.kids.domain.model.ColoringTool
import com.colormagic.kids.presentation.screens.coloring.Stroke

// Renders the colored canvas into a single Android Bitmap for saving + sharing.
// This is the OFF-SCREEN twin of SketchCanvas — it must reproduce exactly what
// the kid sees, so the layer order, brush widths, tool blends and the fillable
// mask all mirror SketchCanvas.kt 1:1.
//
// Layer order, bottom → top (must match the on-screen canvas):
//   1. White background.
//   2. The black line-art sketch (its opaque white background fills the page).
//   3. The kid's paint — built in an isolated layer so per-tool blend modes
//      behave like the on-screen offscreen group, then clipped to the fillable
//      mask (DST_IN) so colour never covers a line, and finally composited on
//      TOP of the line art.
//
// NOTE: the line art is drawn UNDER the paint (not on top). The Cloudflare
// coloring page is fully opaque, so drawing it on top would paint white over
// every stroke — which is exactly the "saved without colour" bug this fixes.
object ArtworkRenderer {

    fun render(
        sketchImage: ImageBitmap,
        fillableMask: ImageBitmap?,
        strokes: List<Stroke>,
        canvasWidthPx: Int,
        canvasHeightPx: Int,
        densityScale: Float
    ): Bitmap {
        require(canvasWidthPx > 0 && canvasHeightPx > 0) {
            "Render target must have positive dimensions"
        }
        val result = Bitmap.createBitmap(
            canvasWidthPx, canvasHeightPx, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        canvas.drawColor(android.graphics.Color.WHITE)

        // 1. Line art at the bottom.
        val scaledSketch = Bitmap.createScaledBitmap(
            sketchImage.asAndroidBitmap(), canvasWidthPx, canvasHeightPx, true
        )
        canvas.drawBitmap(scaledSketch, 0f, 0f, null)

        // 2. Paint in its own isolated layer so tool blend modes only interact
        //    with other strokes (never the line art) — like the on-screen
        //    offscreen group.
        val strokeLayer = Bitmap.createBitmap(
            canvasWidthPx, canvasHeightPx, Bitmap.Config.ARGB_8888
        )
        val strokeCanvas = Canvas(strokeLayer)
        strokes.forEach { drawStroke(strokeCanvas, it, densityScale) }

        // 3. Clip paint to the fillable (non-line) pixels.
        if (fillableMask != null) {
            val scaledMask = Bitmap.createScaledBitmap(
                fillableMask.asAndroidBitmap(), canvasWidthPx, canvasHeightPx, true
            )
            val maskPaint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            strokeCanvas.drawBitmap(scaledMask, 0f, 0f, maskPaint)
        }

        // 4. Paint on top of the line art (mask kept it off the lines).
        canvas.drawBitmap(strokeLayer, 0f, 0f, null)

        return result
    }

    /** Brush base widths in canvas px — mirrors SketchCanvas.strokePx(). */
    private fun baseWidthPx(size: BrushSize, densityScale: Float): Float = when (size) {
        BrushSize.XSmall -> 4f
        BrushSize.Small -> 10f
        BrushSize.Medium -> 18f
        BrushSize.Large -> 28f
    } * densityScale

    // Replicates SketchCanvas.drawStroke for each tool.
    private fun drawStroke(canvas: Canvas, stroke: Stroke, densityScale: Float) {
        if (stroke.points.isEmpty()) return
        val w = baseWidthPx(stroke.size, densityScale)
        val base = stroke.colorArgb.toInt()

        when (stroke.tool) {
            ColoringTool.Marker ->
                segmented(canvas, stroke, w) { base }

            ColoringTool.Crayon -> {
                segmented(canvas, stroke, w * 1.05f) { withAlpha(base, 0.55f) }
                segmented(canvas, stroke, w * 0.75f) { withAlpha(base, 0.85f) }
            }

            ColoringTool.Pencil ->
                segmented(canvas, stroke, w * 0.55f) { withAlpha(base, 0.70f) }

            ColoringTool.Watercolor -> {
                segmented(canvas, stroke, w * 1.6f) { withAlpha(base, 0.22f) }
                segmented(canvas, stroke, w * 0.9f) { withAlpha(base, 0.38f) }
            }

            ColoringTool.Highlighter ->
                segmented(canvas, stroke, w * 1.8f, PorterDuff.Mode.MULTIPLY) {
                    withAlpha(base, 0.55f)
                }

            ColoringTool.Magic ->
                segmented(canvas, stroke, w) { index -> MAGIC_HUES[index % MAGIC_HUES.size] }

            ColoringTool.Glitter -> {
                // Faint trail + deterministic twinkles — mirrors SketchCanvas.
                segmented(canvas, stroke, w * 0.45f) { withAlpha(base, 0.35f) }
                val sparkle = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                }
                stroke.points.forEachIndexed { i, p ->
                    val r = if (i % 2 == 0) w * 0.38f else w * 0.22f
                    sparkle.color = if (i % 3 == 0) android.graphics.Color.WHITE else base
                    canvas.drawCircle(p.x, p.y, r, sparkle)
                }
            }

            ColoringTool.Eraser ->
                segmented(canvas, stroke, w * 1.6f, PorterDuff.Mode.CLEAR) { base }

            ColoringTool.Fill -> stroke.points.firstOrNull()?.let { p ->
                val paint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = base
                }
                canvas.drawCircle(p.x, p.y, w * 5.5f, paint)
            }
        }
    }

    /** Draws a stroke as connected round-capped segments (or a dot for a tap),
     *  mirroring SketchCanvas.drawSegmented. [colorFor] receives the segment
     *  index so Magic can cycle hues along the line. */
    private inline fun segmented(
        canvas: Canvas,
        stroke: Stroke,
        strokeWidth: Float,
        blend: PorterDuff.Mode? = null,
        colorFor: (segmentIndex: Int) -> Int
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth
            if (blend != null) xfermode = PorterDuffXfermode(blend)
        }
        val pts = stroke.points
        if (pts.size == 1) {
            paint.style = Paint.Style.FILL
            paint.color = colorFor(0)
            val p = pts.first()
            canvas.drawCircle(p.x, p.y, strokeWidth / 2f, paint)
            return
        }
        for (i in 1 until pts.size) {
            paint.color = colorFor(i)
            val a = pts[i - 1]
            val b = pts[i]
            canvas.drawLine(a.x, a.y, b.x, b.y, paint)
        }
    }

    /** Sets the alpha channel to [fraction] (0..1), matching Compose's
     *  Color.copy(alpha = …) which replaces (not multiplies) alpha. */
    private fun withAlpha(argb: Int, fraction: Float): Int {
        val a = (fraction.coerceIn(0f, 1f) * 255f).toInt()
        return (argb and 0x00FFFFFF) or (a shl 24)
    }

    private val MAGIC_HUES: List<Int> = listOf(
        0xFFEF5350.toInt(), // red
        0xFFFFA726.toInt(), // orange
        0xFFFFEB3B.toInt(), // yellow
        0xFF66BB6A.toInt(), // green
        0xFF26A69A.toInt(), // teal
        0xFF42A5F5.toInt(), // blue
        0xFF7E57C2.toInt(), // purple
        0xFFEC407A.toInt()  // pink
    )
}
