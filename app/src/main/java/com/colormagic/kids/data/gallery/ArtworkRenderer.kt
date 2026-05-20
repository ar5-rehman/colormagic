package com.colormagic.kids.data.gallery

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.colormagic.kids.domain.model.BrushSize
import com.colormagic.kids.presentation.screens.coloring.Stroke

// Renders the colored canvas into a single Android Bitmap suitable for
// saving + sharing. Mirrors what SketchCanvas draws on screen, just into
// an off-screen bitmap so we can write a PNG out.
//
// Layer order, bottom to top — match the on-screen canvas:
//   1. White background.
//   2. Stroke layer (all completed strokes) — paint is then clipped to the
//      fillable mask via PorterDuff.DST_IN so colour never escapes a line.
//   3. The black line-art sketch — drawn last so lines sit on top of paint.
object ArtworkRenderer {

    fun render(
        sketchImage: ImageBitmap,
        fillableMask: ImageBitmap?,
        strokes: List<Stroke>,
        canvasWidthPx: Int,
        canvasHeightPx: Int
    ): Bitmap {
        require(canvasWidthPx > 0 && canvasHeightPx > 0) {
            "Render target must have positive dimensions"
        }
        val result = Bitmap.createBitmap(
            canvasWidthPx, canvasHeightPx, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        canvas.drawColor(android.graphics.Color.WHITE)

        // 1. Strokes layered into an intermediate bitmap so we can DST_IN
        //    against the mask in one go — masking each stroke individually
        //    would be much slower.
        val strokeLayer = Bitmap.createBitmap(
            canvasWidthPx, canvasHeightPx, Bitmap.Config.ARGB_8888
        )
        val strokeCanvas = Canvas(strokeLayer)
        strokes.forEach { drawStroke(strokeCanvas, it) }

        if (fillableMask != null) {
            // Mask is opaque on paintable pixels, transparent on lines.
            // DST_IN keeps stroke pixels only where the mask is opaque.
            val scaledMask = Bitmap.createScaledBitmap(
                fillableMask.asAndroidBitmap(),
                canvasWidthPx, canvasHeightPx, true
            )
            val maskPaint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            strokeCanvas.drawBitmap(scaledMask, 0f, 0f, maskPaint)
        }

        canvas.drawBitmap(strokeLayer, 0f, 0f, null)

        // 2. Line art on top so the kid's lines stay visible above their paint.
        val scaledSketch = Bitmap.createScaledBitmap(
            sketchImage.asAndroidBitmap(),
            canvasWidthPx, canvasHeightPx, true
        )
        canvas.drawBitmap(scaledSketch, 0f, 0f, null)

        return result
    }

    /** Stroke widths in canvas pixels — match SketchCanvas's on-screen sizes. */
    private fun brushWidthPx(size: BrushSize): Float = when (size) {
        BrushSize.XSmall -> 10f
        BrushSize.Small -> 18f
        BrushSize.Medium -> 32f
        BrushSize.Large -> 54f
    }

    private fun drawStroke(canvas: Canvas, stroke: Stroke) {
        if (stroke.points.isEmpty()) return
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = stroke.colorArgb.toInt()
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = brushWidthPx(stroke.size)
        }
        if (stroke.points.size == 1) {
            // Single-tap stroke — draw it as a filled dot.
            val p = stroke.points.first()
            paint.style = Paint.Style.FILL
            canvas.drawCircle(p.x, p.y, paint.strokeWidth / 2f, paint)
            return
        }
        val path = Path().apply {
            moveTo(stroke.points.first().x, stroke.points.first().y)
            for (i in 1 until stroke.points.size) {
                lineTo(stroke.points[i].x, stroke.points[i].y)
            }
        }
        canvas.drawPath(path, paint)
    }
}
