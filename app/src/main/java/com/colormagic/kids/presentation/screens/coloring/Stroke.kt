package com.colormagic.kids.presentation.screens.coloring

import com.colormagic.kids.domain.model.BrushSize
import com.colormagic.kids.domain.model.ColoringTool

data class StrokePoint(val x: Float, val y: Float)

enum class TextFont(val label: String) {
    Normal("Normal"),
    Bold("Bold"),
    Italic("Italic"),
    BoldItalic("Bold Italic")
}

enum class ArtworkAnimation(val label: String, val emoji: String) {
    None("None", "⏹"),
    Bounce("Bounce", "🏀"),
    Float("Float", "🎈"),
    Wiggle("Wiggle", "🐛"),
    Spin("Spin", "🌀"),
    Heartbeat("Heartbeat", "💓"),
    Jelly("Jelly", "🍮")
}

data class Stroke(
    val tool: ColoringTool,
    val colorArgb: Long,
    val size: BrushSize,
    val points: List<StrokePoint>,
    val strokeWidthBase: Float = -1f,
    val opacity: Float = 1f,
    val text: String? = null,
    val textSizeSp: Float = 28f,
    val textFont: TextFont = TextFont.Normal
)

fun Stroke.effectiveWidthPx(densityScale: Float): Float {
    if (strokeWidthBase > 0f) return strokeWidthBase * densityScale
    return size.baseWidthPx * densityScale
}

val BrushSize.baseWidthPx: Float
    get() = when (this) {
        BrushSize.XSmall -> 4f
        BrushSize.Small -> 10f
        BrushSize.Medium -> 18f
        BrushSize.Large -> 28f
    }
