package com.colormagic.kids.domain.model

// What the kid is currently doing on the canvas.
//
// Brush family — render an actual coloured stroke. Each one has a distinct
// visual character so kids feel like they're switching real art supplies:
//   Crayon      — waxy, slightly grainy, kid-friendly default
//   Marker      — bold solid line
//   Pencil      — thin, sketchy, semi-transparent
//   Watercolor  — soft wide stroke at low opacity, builds up with layering
//   Highlighter — wide stroke that multiplies onto what's underneath
//   Magic       — rainbow brush, colour cycles along the stroke
//
// Action tools — change the canvas in special ways:
//   Fill   — tap to drop a coloured spot
//   Eraser — remove paint
enum class ColoringTool {
    Crayon,
    Marker,
    Pencil,
    Watercolor,
    Highlighter,
    Magic,
    Glitter,    // sparkly trail of twinkles along the stroke
    Fill,
    Eraser
}

// True for every tool that puts pigment down by tracking a finger path.
// Used by the UI to group brush variants separately from action tools.
val ColoringTool.isBrush: Boolean
    get() = this in setOf(
        ColoringTool.Crayon,
        ColoringTool.Marker,
        ColoringTool.Pencil,
        ColoringTool.Watercolor,
        ColoringTool.Highlighter,
        ColoringTool.Magic,
        ColoringTool.Glitter
    )

enum class BrushSize { XSmall, Small, Medium, Large }

// One swatch in the palette. Hex stored as Long so the domain stays
// independent of Compose's Color type.
data class PaintColor(
    val id: String,
    val name: String,
    val argb: Long
)

object DefaultPalette {
    // 16 kid-friendly hues. Order = visible rainbow → earth tones → neutrals,
    // so the row scrolls intuitively from "hot" to "cool" colours.
    val colors: List<PaintColor> = listOf(
        PaintColor("red", "Red", 0xFFEF5350),
        PaintColor("coral", "Coral", 0xFFFF7043),
        PaintColor("orange", "Orange", 0xFFFFA726),
        PaintColor("yellow", "Yellow", 0xFFFFEB3B),
        PaintColor("lime", "Lime", 0xFFCDDC39),
        PaintColor("green", "Green", 0xFF66BB6A),
        PaintColor("mint", "Mint", 0xFF26A69A),
        PaintColor("teal", "Teal", 0xFF00ACC1),
        PaintColor("sky", "Sky", 0xFF29B6F6),
        PaintColor("blue", "Blue", 0xFF42A5F5),
        PaintColor("indigo", "Indigo", 0xFF5C6BC0),
        PaintColor("violet", "Violet", 0xFFAB47BC),
        PaintColor("pink", "Pink", 0xFFEC407A),
        PaintColor("rose", "Rose", 0xFFF06292),
        PaintColor("brown", "Brown", 0xFF8D6E63),
        PaintColor("black", "Black", 0xFF424242)
    )
}
