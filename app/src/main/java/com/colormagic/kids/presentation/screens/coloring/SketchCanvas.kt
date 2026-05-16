package com.colormagic.kids.presentation.screens.coloring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.colormagic.kids.R
import com.colormagic.kids.domain.model.BrushSize
import com.colormagic.kids.domain.model.ColoringTool
import com.colormagic.kids.domain.model.PaintColor

// How far the paint cursor leads the kid's fingertip — the "finger-lift offset".
//
// Roughly thumb-tip height. Small enough that the gesture still feels direct
// (the cursor visibly tracks the finger), large enough that the painted pixels
// always sit ABOVE the finger so they're visible.
//
// Set to 0.dp to disable lift entirely (paint lands exactly under the finger).
// A parent-controlled setting would live in Preferences and flow in via VM.
private val FINGER_LIFT_OFFSET = 28.dp

// Stops the lift from running the cursor off the top of the canvas — keeps
// at least this much room from the top edge.
private val EDGE_PADDING = 6.dp

// The kid-facing drawing surface.
//
// Layers (back to front):
//   1) White background.
//   2) sketch.png — the line drawing the kid is colouring.
//   3) Drawing Canvas — offscreen layer for stroke history + fillable mask.
//      The mask clips paint to non-line pixels (DstIn blend).
//   4) Brush preview overlay — NOT in the offscreen layer. Renders a coloured
//      halo at the paint position so the kid can see brush size + colour
//      even when their finger blocks the centre. Stays visible while pressing.
@Composable
fun SketchCanvas(
    tool: ColoringTool,
    selectedColor: PaintColor,
    brushSize: BrushSize,
    strokes: List<Stroke>,
    fillableMask: ImageBitmap?,
    onStrokeFinished: (Stroke) -> Unit,
    modifier: Modifier = Modifier
) {
    var inProgress by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    // Tracks the live paint position so the overlay halo follows the finger.
    // Null when no finger is on the canvas → overlay hides.
    var cursor by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    val liftPx = with(density) { FINGER_LIFT_OFFSET.toPx() }
    val edgePadPx = with(density) { EDGE_PADDING.toPx() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(R.drawable.sketch),
            contentDescription = "Coloring sketch",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        // The drawing layer (inside offscreen so the mask works).
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .pointerInput(tool, selectedColor.id, brushSize) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val paintPos = down.position.liftedBy(liftPx, edgePadPx)
                        cursor = paintPos
                        inProgress = listOf(StrokePoint(paintPos.x, paintPos.y))
                        down.consume()

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.pressed) {
                                val lifted = change.position.liftedBy(liftPx, edgePadPx)
                                val newPoint = StrokePoint(lifted.x, lifted.y)
                                val last = inProgress.lastOrNull()
                                if (last == null || last.x != newPoint.x || last.y != newPoint.y) {
                                    inProgress = inProgress + newPoint
                                    cursor = lifted
                                }
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed && it.id == down.id })

                        if (inProgress.isNotEmpty()) {
                            onStrokeFinished(
                                Stroke(
                                    tool = tool,
                                    colorArgb = selectedColor.argb,
                                    size = brushSize,
                                    points = inProgress
                                )
                            )
                            inProgress = emptyList()
                        }
                        // Hide the halo once the finger lifts.
                        cursor = null
                    }
                }
        ) {
            strokes.forEach { drawStroke(it, density.density) }
            if (inProgress.isNotEmpty()) {
                drawStroke(
                    stroke = Stroke(
                        tool = tool,
                        colorArgb = selectedColor.argb,
                        size = brushSize,
                        points = inProgress
                    ),
                    densityScale = density.density
                )
            }
            fillableMask?.let { mask ->
                drawImage(
                    image = mask,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(mask.width, mask.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    blendMode = BlendMode.DstIn,
                    filterQuality = FilterQuality.Low
                )
            }
        }

        // Brush preview overlay — its own Canvas (NOT offscreen) so the halo
        // is visible everywhere, including over lines and outside paintable
        // areas. The kid always sees what their finger is doing.
        Canvas(modifier = Modifier.fillMaxSize()) {
            cursor?.let { position ->
                drawBrushPreview(
                    center = position,
                    tool = tool,
                    color = selectedColor,
                    size = brushSize,
                    densityScale = density.density
                )
            }
        }
    }
}

// Pull the actual paint position up by [liftPx] dp so the cursor leads the
// finger. Clamped to [edgePadPx] from the top so a stroke near the top edge
// doesn't run off the canvas.
private fun Offset.liftedBy(liftPx: Float, edgePadPx: Float): Offset =
    Offset(x, (y - liftPx).coerceAtLeast(edgePadPx))

private fun DrawScope.drawBrushPreview(
    center: Offset,
    tool: ColoringTool,
    color: PaintColor,
    size: BrushSize,
    densityScale: Float
) {
    // Preview size matches the actual stroke width for each tool.
    val baseWidth = size.strokePx(densityScale)
    val previewRadius = when (tool) {
        ColoringTool.Watercolor -> baseWidth * 0.8f
        ColoringTool.Highlighter -> baseWidth * 0.9f
        ColoringTool.Fill -> baseWidth * 1.4f          // fill is bigger than the brush
        ColoringTool.Pencil -> baseWidth * 0.28f       // pencil is thinner
        else -> baseWidth / 2f
    }
    val haloRadius = previewRadius + 3.dp.toPx()

    // Inner colour fill — tinted by the active tool so the kid sees the
    // colour they're about to commit. Alpha matches the brush's character:
    // watercolor preview is softer than marker, etc.
    val fillColor = when (tool) {
        ColoringTool.Eraser -> Color(0xFFE0E0E0)
        ColoringTool.Magic -> Color(0xFFB39DDB)
        else -> Color(color.argb)
    }
    val previewAlpha = when (tool) {
        ColoringTool.Watercolor, ColoringTool.Highlighter -> 0.22f
        ColoringTool.Pencil -> 0.5f
        else -> 0.35f
    }
    drawCircle(
        color = fillColor.copy(alpha = previewAlpha),
        radius = previewRadius,
        center = center
    )

    // Double ring — one bright, one dark — so the halo is visible against
    // *any* background (white paper, dark lines, painted pixels, photos).
    drawCircle(
        color = Color.White,
        radius = haloRadius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.55f),
        radius = haloRadius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
    )

    // Eraser also gets a diagonal slash inside the halo so it reads as
    // "removing" rather than "painting grey".
    if (tool == ColoringTool.Eraser) {
        val r = previewRadius * 0.6f
        drawLine(
            color = Color(0xFFC62828),
            start = Offset(center.x - r, center.y - r),
            end = Offset(center.x + r, center.y + r),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawStroke(stroke: Stroke, densityScale: Float) {
    val w = stroke.size.strokePx(densityScale)
    val baseColor = Color(stroke.colorArgb)
    when (stroke.tool) {
        // ─── Marker ─── bold opaque, smooth round-cap line.
        ColoringTool.Marker -> drawSegmented(
            stroke = stroke,
            strokeWidth = w,
            colorFor = { baseColor }
        )

        // ─── Crayon ─── two layered strokes for a waxy build-up. The wider
        // base is partly transparent; a slightly thinner overlay sits on top.
        ColoringTool.Crayon -> {
            drawSegmented(stroke, w * 1.05f) { baseColor.copy(alpha = 0.55f) }
            drawSegmented(stroke, w * 0.75f) { baseColor.copy(alpha = 0.85f) }
        }

        // ─── Pencil ─── thin and translucent so it reads like graphite.
        ColoringTool.Pencil -> drawSegmented(
            stroke = stroke,
            strokeWidth = w * 0.55f,
            colorFor = { baseColor.copy(alpha = 0.7f) }
        )

        // ─── Watercolor ─── wide soft wash + tighter inner core, both at low
        // opacity so repeated strokes layer up like real paint.
        ColoringTool.Watercolor -> {
            drawSegmented(stroke, w * 1.6f) { baseColor.copy(alpha = 0.22f) }
            drawSegmented(stroke, w * 0.9f) { baseColor.copy(alpha = 0.38f) }
        }

        // ─── Highlighter ─── wide stroke with Multiply so lines and prior
        // colour underneath show through, tinted by the highlighter colour.
        ColoringTool.Highlighter -> drawSegmented(
            stroke = stroke,
            strokeWidth = w * 1.8f,
            blendMode = BlendMode.Multiply,
            colorFor = { baseColor.copy(alpha = 0.55f) }
        )

        // ─── Magic ─── colour cycles along the stroke.
        ColoringTool.Magic -> drawSegmented(
            stroke = stroke,
            strokeWidth = w,
            colorFor = { index -> Color(MAGIC_HUES[index % MAGIC_HUES.size]) }
        )

        // ─── Eraser ─── clears paint pixels (offscreen-layer blend).
        ColoringTool.Eraser -> drawSegmented(
            stroke = stroke,
            strokeWidth = w * 1.6f,
            colorFor = { Color.Transparent },
            blendMode = BlendMode.Clear
        )

        // ─── Fill ─── one big coloured spot at the press point.
        ColoringTool.Fill -> {
            stroke.points.firstOrNull()?.let { p ->
                drawCircle(
                    color = baseColor,
                    radius = w * 5.5f,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}

private inline fun DrawScope.drawSegmented(
    stroke: Stroke,
    strokeWidth: Float,
    blendMode: BlendMode = BlendMode.SrcOver,
    crossinline colorFor: (segmentIndex: Int) -> Color
) {
    if (stroke.points.size == 1) {
        val p = stroke.points.first()
        drawCircle(
            color = colorFor(0),
            radius = strokeWidth / 2f,
            center = Offset(p.x, p.y),
            blendMode = blendMode
        )
        return
    }
    for (i in 1 until stroke.points.size) {
        val a = stroke.points[i - 1]
        val b = stroke.points[i]
        drawLine(
            color = colorFor(i),
            start = Offset(a.x, a.y),
            end = Offset(b.x, b.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
            blendMode = blendMode
        )
    }
}

private val MAGIC_HUES: List<Long> = listOf(
    0xFFEF5350, // red
    0xFFFFA726, // orange
    0xFFFFEB3B, // yellow
    0xFF66BB6A, // green
    0xFF26A69A, // teal
    0xFF42A5F5, // blue
    0xFF7E57C2, // purple
    0xFFEC407A  // pink
)

private fun BrushSize.strokePx(densityScale: Float): Float = when (this) {
    BrushSize.XSmall -> 4f
    BrushSize.Small -> 10f
    BrushSize.Medium -> 18f
    BrushSize.Large -> 28f
} * densityScale
