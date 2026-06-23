package com.colormagic.kids.presentation.screens.coloring

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ColorRegion(
    val number: Int,
    val centroidX: Float,
    val centroidY: Float,
    val assignedColor: Long,
    val pixelCount: Int
)

private val CBN_COLORS: List<Long> = listOf(
    0xFFEF5350, // 1  Red
    0xFF42A5F5, // 2  Blue
    0xFF66BB6A, // 3  Green
    0xFFFFEB3B, // 4  Yellow
    0xFFAB47BC, // 5  Purple
    0xFFFFA726, // 6  Orange
    0xFF26A69A, // 7  Teal
    0xFFEC407A, // 8  Pink
    0xFF8D6E63, // 9  Brown
    0xFF29B6F6, // 10 Sky
    0xFFCDDC39, // 11 Lime
    0xFFFF7043, // 12 Coral
    0xFF5C6BC0, // 13 Indigo
    0xFFF06292, // 14 Rose
    0xFF00ACC1, // 15 Cyan
    0xFFFFCA28, // 16 Amber
    0xFF9CCC65, // 17 Light Green
    0xFFBA68C8, // 18 Orchid
    0xFF78909C, // 19 Blue Grey
    0xFFFF8A65, // 20 Deep Orange
)

object RegionDetector {

    private const val ANALYSIS_SIZE = 200
    private const val MIN_REGION_FRACTION = 0.002f
    private const val MAX_REGIONS = 20

    suspend fun detect(fillableMask: ImageBitmap): List<ColorRegion> =
        withContext(Dispatchers.Default) {
            val source = fillableMask.asAndroidBitmap()
            val w = ANALYSIS_SIZE
            val h = (source.height.toFloat() / source.width * w).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(source, w, h, false)

            val pixels = IntArray(w * h)
            scaled.getPixels(pixels, 0, w, 0, 0, w, h)

            val labels = IntArray(w * h) { -1 }
            var nextLabel = 0
            val regionPixels = mutableMapOf<Int, MutableList<Int>>()

            for (i in pixels.indices) {
                if (labels[i] != -1) continue
                val alpha = (pixels[i] ushr 24) and 0xFF
                if (alpha < 128) {
                    labels[i] = -2
                    continue
                }
                val label = nextLabel++
                val collected = mutableListOf<Int>()
                floodFill(pixels, labels, w, h, i, label, collected)
                regionPixels[label] = collected
            }

            val totalPixels = w * h
            val minPixels = (totalPixels * MIN_REGION_FRACTION).toInt()

            val sorted = regionPixels.entries
                .filter { it.value.size >= minPixels }
                .sortedByDescending { it.value.size }
                .take(MAX_REGIONS)

            val scaleX = 1f / w
            val scaleY = 1f / h

            sorted.mapIndexed { idx, (_, pxList) ->
                var sumX = 0L
                var sumY = 0L
                for (pi in pxList) {
                    sumX += pi % w
                    sumY += pi / w
                }
                val cx = (sumX.toFloat() / pxList.size) * scaleX
                val cy = (sumY.toFloat() / pxList.size) * scaleY
                ColorRegion(
                    number = idx + 1,
                    centroidX = cx,
                    centroidY = cy,
                    assignedColor = CBN_COLORS[idx % CBN_COLORS.size],
                    pixelCount = pxList.size
                )
            }
        }

    private fun floodFill(
        pixels: IntArray,
        labels: IntArray,
        w: Int,
        h: Int,
        start: Int,
        label: Int,
        collected: MutableList<Int>
    ) {
        val stack = ArrayDeque<Int>(256)
        stack.addLast(start)
        labels[start] = label
        collected.add(start)

        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            val x = idx % w
            val y = idx / w

            val neighbors = intArrayOf(
                if (x > 0) idx - 1 else -1,
                if (x < w - 1) idx + 1 else -1,
                if (y > 0) idx - w else -1,
                if (y < h - 1) idx + w else -1
            )

            for (ni in neighbors) {
                if (ni < 0 || labels[ni] != -1) continue
                val alpha = (pixels[ni] ushr 24) and 0xFF
                if (alpha < 128) {
                    labels[ni] = -2
                    continue
                }
                labels[ni] = label
                collected.add(ni)
                stack.addLast(ni)
            }
        }
    }
}
