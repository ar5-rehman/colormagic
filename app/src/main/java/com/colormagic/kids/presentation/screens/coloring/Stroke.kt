package com.colormagic.kids.presentation.screens.coloring

import com.colormagic.kids.domain.model.BrushSize
import com.colormagic.kids.domain.model.ColoringTool

// One thing the kid drew on the canvas — a single press-drag-release.
//
// Points are stored in pixel coordinates relative to the canvas size at the
// time of capture. If the canvas is resized (rotation, screen split), the
// strokes will still draw — they just won't precisely re-fit the new bounds.
//
// Stored here rather than in domain/ because it's a presentation concern
// (Compose Offset / pixel coordinates), not a backend-shaped concept.
data class StrokePoint(val x: Float, val y: Float)

data class Stroke(
    val tool: ColoringTool,
    val colorArgb: Long,
    val size: BrushSize,
    val points: List<StrokePoint>
)
