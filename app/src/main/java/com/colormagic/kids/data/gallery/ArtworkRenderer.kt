package com.colormagic.kids.data.gallery

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.colormagic.kids.domain.model.ColoringTool
import com.colormagic.kids.presentation.screens.coloring.Stroke
import com.colormagic.kids.presentation.screens.coloring.effectiveWidthPx
import com.colormagic.kids.presentation.screens.coloring.toTypeface

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

        val scaledSketch = Bitmap.createScaledBitmap(
            sketchImage.asAndroidBitmap(), canvasWidthPx, canvasHeightPx, true
        )
        canvas.drawBitmap(scaledSketch, 0f, 0f, null)

        val strokeLayer = Bitmap.createBitmap(
            canvasWidthPx, canvasHeightPx, Bitmap.Config.ARGB_8888
        )
        val strokeCanvas = Canvas(strokeLayer)
        strokes.forEach { drawStroke(strokeCanvas, it, densityScale) }

        if (fillableMask != null) {
            val scaledMask = Bitmap.createScaledBitmap(
                fillableMask.asAndroidBitmap(), canvasWidthPx, canvasHeightPx, true
            )
            val maskPaint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            strokeCanvas.drawBitmap(scaledMask, 0f, 0f, maskPaint)
        }

        canvas.drawBitmap(strokeLayer, 0f, 0f, null)

        // Text strokes rendered on top (not clipped by mask)
        strokes.filter { it.tool == ColoringTool.TextTool && it.text != null }.forEach { stroke ->
            val p = stroke.points.firstOrNull() ?: return@forEach
            val textPaint = Paint().apply {
                color = stroke.colorArgb.toInt()
                textSize = stroke.textSizeSp * densityScale
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                alpha = (stroke.opacity * 255).toInt()
                typeface = stroke.textFont.toTypeface()
            }
            canvas.drawText(stroke.text!!, p.x, p.y, textPaint)
        }

        return result
    }

    private fun drawStroke(canvas: Canvas, stroke: Stroke, densityScale: Float) {
        if (stroke.points.isEmpty()) return
        if (stroke.tool == ColoringTool.TextTool || stroke.tool == ColoringTool.Eyedropper) return

        val w = stroke.effectiveWidthPx(densityScale)
        val base = stroke.colorArgb.toInt()
        val alpha = stroke.opacity

        when (stroke.tool) {
            ColoringTool.Marker ->
                segmented(canvas, stroke, w) { withAlpha(base, alpha) }

            ColoringTool.Crayon -> {
                segmented(canvas, stroke, w * 1.05f) { withAlpha(base, 0.55f * alpha) }
                segmented(canvas, stroke, w * 0.75f) { withAlpha(base, 0.85f * alpha) }
            }

            ColoringTool.Pencil ->
                segmented(canvas, stroke, w * 0.55f) { withAlpha(base, 0.70f * alpha) }

            ColoringTool.Watercolor -> {
                segmented(canvas, stroke, w * 1.6f) { withAlpha(base, 0.22f * alpha) }
                segmented(canvas, stroke, w * 0.9f) { withAlpha(base, 0.38f * alpha) }
            }

            ColoringTool.Highlighter ->
                segmented(canvas, stroke, w * 1.8f, PorterDuff.Mode.MULTIPLY) {
                    withAlpha(base, 0.55f * alpha)
                }

            ColoringTool.Magic ->
                segmented(canvas, stroke, w) { index ->
                    withAlpha(MAGIC_HUES[index % MAGIC_HUES.size], alpha)
                }

            ColoringTool.Glitter -> {
                segmented(canvas, stroke, w * 0.45f) { withAlpha(base, 0.35f * alpha) }
                val sparkle = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                }
                stroke.points.forEachIndexed { i, p ->
                    val r = if (i % 2 == 0) w * 0.38f else w * 0.22f
                    sparkle.color = if (i % 3 == 0) {
                        withAlpha(android.graphics.Color.WHITE, alpha)
                    } else {
                        withAlpha(base, alpha)
                    }
                    canvas.drawCircle(p.x, p.y, r, sparkle)
                }
            }

            ColoringTool.Eraser ->
                segmented(canvas, stroke, w * 1.6f, PorterDuff.Mode.CLEAR) { base }

            ColoringTool.Fill -> stroke.points.firstOrNull()?.let { p ->
                val paint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = withAlpha(base, alpha)
                }
                canvas.drawCircle(p.x, p.y, w * 5.5f, paint)
            }

            else -> {}
        }
    }

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

    private fun withAlpha(argb: Int, fraction: Float): Int {
        val a = (fraction.coerceIn(0f, 1f) * 255f).toInt()
        return (argb and 0x00FFFFFF) or (a shl 24)
    }

    private val MAGIC_HUES: List<Int> = listOf(
        0xFFEF5350.toInt(), 0xFFFFA726.toInt(), 0xFFFFEB3B.toInt(), 0xFF66BB6A.toInt(),
        0xFF26A69A.toInt(), 0xFF42A5F5.toInt(), 0xFF7E57C2.toInt(), 0xFFEC407A.toInt()
    )
}
