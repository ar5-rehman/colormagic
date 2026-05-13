package com.colormagic.kids.domain.model

enum class ColoringTool { Brush, Eraser }

enum class BrushSize { XSmall, Small, Medium, Large }

// One swatch in the palette. Hex stored as Long so the domain stays
// independent of Compose's Color type.
data class PaintColor(
    val id: String,
    val name: String,
    val argb: Long
)

object DefaultPalette {
    val colors: List<PaintColor> = listOf(
        PaintColor("red", "Red", 0xFFEF5350),
        PaintColor("orange", "Orange", 0xFFFFA726),
        PaintColor("yellow", "Yellow", 0xFFFFEB3B),
        PaintColor("lime", "Lime", 0xFFCDDC39),
        PaintColor("green", "Green", 0xFF66BB6A),
        PaintColor("teal", "Teal", 0xFF26A69A),
        PaintColor("blue", "Blue", 0xFF42A5F5),
        PaintColor("indigo", "Indigo", 0xFF5C6BC0),
        PaintColor("violet", "Violet", 0xFFAB47BC),
        PaintColor("pink", "Pink", 0xFFEC407A)
    )
}
