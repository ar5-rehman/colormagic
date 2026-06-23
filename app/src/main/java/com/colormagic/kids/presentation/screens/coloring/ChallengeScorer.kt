package com.colormagic.kids.presentation.screens.coloring

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.colormagic.kids.domain.model.ColoringTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ChallengeScore(
    val coveragePercent: Int,
    val lineRespectPercent: Int,
    val colorVariety: Int,
    val totalScore: Int,
    val stars: Int,
    val title: String,
    val emoji: String
)

object ChallengeScorer {

    suspend fun score(
        fillableMask: ImageBitmap?,
        strokes: List<Stroke>,
        canvasWidth: Int,
        canvasHeight: Int,
        densityScale: Float
    ): ChallengeScore = withContext(Dispatchers.Default) {
        val coverage = computeCoverage(fillableMask, strokes, canvasWidth, canvasHeight, densityScale)
        val lineRespect = computeLineRespect(fillableMask, strokes, canvasWidth, canvasHeight, densityScale)
        val variety = computeColorVariety(strokes)

        val coverageScore = (coverage * 40).toInt().coerceIn(0, 40)
        val lineScore = (lineRespect * 35).toInt().coerceIn(0, 35)
        val varietyScore = (variety.coerceAtMost(8) * 25 / 8f).toInt().coerceIn(0, 25)
        val total = coverageScore + lineScore + varietyScore

        val stars = when {
            total >= 85 -> 5
            total >= 70 -> 4
            total >= 55 -> 3
            total >= 35 -> 2
            else -> 1
        }

        val (title, emoji) = when (stars) {
            5 -> "Color Superstar!" to "🌟"
            4 -> "Amazing Artist!" to "🎨"
            3 -> "Great Job!" to "✨"
            2 -> "Nice Try!" to "👏"
            else -> "Keep Coloring!" to "🖍️"
        }

        ChallengeScore(
            coveragePercent = (coverage * 100).toInt(),
            lineRespectPercent = (lineRespect * 100).toInt(),
            colorVariety = variety,
            totalScore = total,
            stars = stars,
            title = title,
            emoji = emoji
        )
    }

    private fun computeCoverage(
        fillableMask: ImageBitmap?,
        strokes: List<Stroke>,
        canvasWidth: Int,
        canvasHeight: Int,
        densityScale: Float
    ): Float {
        if (fillableMask == null || canvasWidth <= 0 || canvasHeight <= 0) return 0f
        val paintStrokes = strokes.filter {
            it.tool != ColoringTool.Eraser && it.tool != ColoringTool.Eyedropper && it.tool != ColoringTool.TextTool
        }
        if (paintStrokes.isEmpty()) return 0f

        val sampleSize = 100
        val mask = Bitmap.createScaledBitmap(fillableMask.asAndroidBitmap(), sampleSize, sampleSize, true)
        val pixels = IntArray(sampleSize * sampleSize)
        mask.getPixels(pixels, 0, sampleSize, 0, 0, sampleSize, sampleSize)

        var fillableCount = 0
        var coveredCount = 0

        for (y in 0 until sampleSize) {
            for (x in 0 until sampleSize) {
                val alpha = (pixels[y * sampleSize + x] ushr 24) and 0xFF
                if (alpha > 128) {
                    fillableCount++
                    val canvasX = (x.toFloat() / sampleSize) * canvasWidth
                    val canvasY = (y.toFloat() / sampleSize) * canvasHeight
                    if (hasStrokeNear(paintStrokes, canvasX, canvasY, densityScale)) {
                        coveredCount++
                    }
                }
            }
        }
        return if (fillableCount == 0) 0f else coveredCount.toFloat() / fillableCount
    }

    private fun hasStrokeNear(strokes: List<Stroke>, x: Float, y: Float, densityScale: Float): Boolean {
        val threshold = 20f * densityScale
        val thresholdSq = threshold * threshold
        for (stroke in strokes) {
            for (p in stroke.points) {
                val dx = p.x - x
                val dy = p.y - y
                if (dx * dx + dy * dy <= thresholdSq) return true
            }
        }
        return false
    }

    private fun computeLineRespect(
        fillableMask: ImageBitmap?,
        strokes: List<Stroke>,
        canvasWidth: Int,
        canvasHeight: Int,
        densityScale: Float
    ): Float {
        if (fillableMask == null || canvasWidth <= 0 || canvasHeight <= 0) return 1f
        val paintStrokes = strokes.filter {
            it.tool != ColoringTool.Eraser && it.tool != ColoringTool.Eyedropper && it.tool != ColoringTool.TextTool
        }
        if (paintStrokes.isEmpty()) return 1f

        val mask = fillableMask.asAndroidBitmap()
        var totalPoints = 0
        var insidePoints = 0

        for (stroke in paintStrokes) {
            for (p in stroke.points) {
                totalPoints++
                val mx = ((p.x / canvasWidth) * mask.width).toInt().coerceIn(0, mask.width - 1)
                val my = ((p.y / canvasHeight) * mask.height).toInt().coerceIn(0, mask.height - 1)
                val pixel = mask.getPixel(mx, my)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha > 128) insidePoints++
            }
        }
        return if (totalPoints == 0) 1f else insidePoints.toFloat() / totalPoints
    }

    private fun computeColorVariety(strokes: List<Stroke>): Int {
        return strokes
            .filter { it.tool != ColoringTool.Eraser && it.tool != ColoringTool.Eyedropper && it.tool != ColoringTool.TextTool }
            .map { it.colorArgb }
            .distinct()
            .size
    }
}
