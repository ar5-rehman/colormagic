package com.colormagic.kids.presentation.screens.coloring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
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
import kotlin.math.roundToInt

private val FINGER_LIFT_OFFSET = 56.dp
private val EDGE_PADDING = 6.dp

@Composable
fun SketchCanvas(
    tool: ColoringTool,
    selectedColor: PaintColor,
    brushSize: BrushSize,
    strokes: List<Stroke>,
    fillableMask: ImageBitmap?,
    onStrokeFinished: (Stroke) -> Unit,
    modifier: Modifier = Modifier,
    sketchImage: ImageBitmap? = null,
    symmetryEnabled: Boolean = false,
    colorByNumberEnabled: Boolean = false,
    colorRegions: List<ColorRegion> = emptyList(),
    zoomScale: Float = 1f,
    zoomOffset: Offset = Offset.Zero,
    onZoomChanged: (Float, Offset) -> Unit = { _, _ -> },
    onEyedropperPick: (Float, Float) -> Unit = { _, _ -> },
    onTextToolTap: (Float, Float) -> Unit = { _, _ -> },
    onTextDragStart: (Float, Float) -> Boolean = { _, _ -> false },
    onTextDragMove: (Float, Float) -> Unit = { _, _ -> },
    onTextDragEnd: () -> Unit = {},
    strokeWidthBase: Float = 18f,
    opacity: Float = 1f
) {
    var inProgress by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    var cursor by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    val liftPx = with(density) { FINGER_LIFT_OFFSET.toPx() }
    val edgePadPx = with(density) { EDGE_PADDING.toPx() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clipToBounds()
    ) {
        // Zoom wrapper — scale controlled by +/- buttons, no gesture interference
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = zoomOffset.x
                    translationY = zoomOffset.y
                }
        ) {
            if (sketchImage != null) {
                Image(
                    bitmap = sketchImage,
                    contentDescription = "Coloring sketch",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                com.colormagic.kids.presentation.components.ShimmerBox(
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Drawing layer
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .pointerInput(tool, selectedColor.id, brushSize, strokeWidthBase, opacity) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)

                            // Eyedropper: single tap picks color
                            if (tool == ColoringTool.Eyedropper) {
                                val pos = down.position.liftedBy(liftPx, edgePadPx)
                                onEyedropperPick(pos.x, pos.y)
                                down.consume()
                                return@awaitEachGesture
                            }

                            // Text tool: use raw position (no lift offset — text
                            // needs to appear and hit-test exactly where the finger is)
                            if (tool == ColoringTool.TextTool) {
                                val pos = down.position
                                val isDrag = onTextDragStart(pos.x, pos.y)
                                if (isDrag) {
                                    down.consume()
                                    do {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                        if (ch.pressed) {
                                            onTextDragMove(ch.position.x, ch.position.y)
                                            ch.consume()
                                        }
                                    } while (ev.changes.any { it.pressed && it.id == down.id })
                                    onTextDragEnd()
                                    return@awaitEachGesture
                                }
                                onTextToolTap(pos.x, pos.y)
                                down.consume()
                                return@awaitEachGesture
                            }

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
                                        points = inProgress,
                                        strokeWidthBase = strokeWidthBase,
                                        opacity = opacity
                                    )
                                )
                                inProgress = emptyList()
                            }
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
                            points = inProgress,
                            strokeWidthBase = strokeWidthBase,
                            opacity = opacity
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

            // Text strokes layer (on top, not in offscreen group so mask doesn't clip text)
            Canvas(modifier = Modifier.fillMaxSize()) {
                strokes.filter { it.tool == ColoringTool.TextTool && it.text != null }.forEach { stroke ->
                    val p = stroke.points.firstOrNull() ?: return@forEach
                    drawIntoCanvas { canvas ->
                        val textPaint = android.graphics.Paint().apply {
                            color = stroke.colorArgb.toInt()
                            textSize = stroke.textSizeSp * density.density
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            alpha = (stroke.opacity * 255).toInt()
                            typeface = stroke.textFont.toTypeface()
                        }
                        canvas.nativeCanvas.drawText(stroke.text!!, p.x, p.y, textPaint)
                    }
                }
            }

            // Cursor overlay
            cursor?.let { position ->
                val brushIcon = tool.brushIconRes()
                if (brushIcon != null) {
                    BrushCursor(
                        iconRes = brushIcon,
                        position = position,
                        dotColor = if (tool == ColoringTool.Magic) Color(0xFFB39DDB)
                        else Color(selectedColor.argb)
                    )
                } else if (tool != ColoringTool.Eyedropper && tool != ColoringTool.TextTool) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawActionCursor(
                            center = position,
                            tool = tool,
                            color = selectedColor,
                            strokeWidthBase = strokeWidthBase,
                            densityScale = density.density
                        )
                    }
                }
            }

            // Symmetry center line
            if (symmetryEnabled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    drawLine(
                        color = Color(0x44AB47BC),
                        start = Offset(cx, 0f),
                        end = Offset(cx, size.height),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(12.dp.toPx(), 8.dp.toPx())
                        )
                    )
                }
            }

            // Color-by-number overlays
            if (colorByNumberEnabled && colorRegions.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 13.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    val bgPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        style = android.graphics.Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val borderPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 1.dp.toPx()
                        isAntiAlias = true
                    }
                    drawIntoCanvas { canvas ->
                        val nCanvas = canvas.nativeCanvas
                        colorRegions.forEach { region ->
                            val cx = region.centroidX * size.width
                            val cy = region.centroidY * size.height
                            val label = region.number.toString()
                            val textWidth = textPaint.measureText(label)
                            val radius = (maxOf(textWidth, textPaint.textSize) / 2f) + 4.dp.toPx()
                            nCanvas.drawCircle(cx, cy, radius, bgPaint)
                            nCanvas.drawCircle(cx, cy, radius, borderPaint)
                            val yOffset = -(textPaint.ascent() + textPaint.descent()) / 2f
                            nCanvas.drawText(label, cx, cy + yOffset, textPaint)
                        }
                    }
                }
            }
        } // end zoom wrapper
    }
}

private fun ColoringTool.brushIconRes(): Int? = when (this) {
    ColoringTool.Crayon -> R.drawable.ic_brush_crayon
    ColoringTool.Marker -> R.drawable.ic_brush_marker
    ColoringTool.Pencil -> R.drawable.ic_brush_pencil
    ColoringTool.Watercolor -> R.drawable.ic_brush_watercolor
    ColoringTool.Highlighter -> R.drawable.ic_brush_highlighter
    ColoringTool.Magic -> R.drawable.ic_brush_magic
    ColoringTool.Glitter -> R.drawable.ic_brush_glitter
    ColoringTool.Fill, ColoringTool.Eraser,
    ColoringTool.Eyedropper, ColoringTool.TextTool -> null
}

private const val BRUSH_TIP_X = 0.38f
private const val BRUSH_TIP_Y = 0.17f
private val BRUSH_CURSOR_SIZE = 84.dp

@Composable
private fun BrushCursor(
    iconRes: Int,
    position: Offset,
    dotColor: Color
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(BRUSH_CURSOR_SIZE)
                .offset {
                    val sizePx = BRUSH_CURSOR_SIZE.toPx()
                    IntOffset(
                        x = (position.x - sizePx * BRUSH_TIP_X).roundToInt(),
                        y = (position.y - sizePx * BRUSH_TIP_Y).roundToInt()
                    )
                }
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color.White, radius = 4.5.dp.toPx(), center = position)
            drawCircle(dotColor, radius = 3f * density, center = position)
        }
    }
}

private fun Offset.liftedBy(liftPx: Float, edgePadPx: Float): Offset =
    Offset(x, (y - liftPx).coerceAtLeast(edgePadPx))

private fun DrawScope.drawActionCursor(
    center: Offset,
    tool: ColoringTool,
    color: PaintColor,
    strokeWidthBase: Float,
    densityScale: Float
) {
    val baseWidth = strokeWidthBase * densityScale
    val radius = when (tool) {
        ColoringTool.Fill -> baseWidth * 1.4f
        else -> baseWidth / 2f
    }
    val haloRadius = radius + 3.dp.toPx()

    val fillColor = if (tool == ColoringTool.Eraser) Color(0xFFE0E0E0) else Color(color.argb)
    drawCircle(color = fillColor.copy(alpha = 0.32f), radius = radius, center = center)
    drawCircle(
        color = Color.White, radius = haloRadius, center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.55f), radius = haloRadius, center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
    )

    if (tool == ColoringTool.Eraser) {
        val r = radius * 0.6f
        drawLine(
            color = Color(0xFFC62828),
            start = Offset(center.x - r, center.y - r),
            end = Offset(center.x + r, center.y + r),
            strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawStroke(stroke: Stroke, densityScale: Float) {
    if (stroke.tool == ColoringTool.TextTool) return
    val w = stroke.effectiveWidthPx(densityScale)
    val baseColor = Color(stroke.colorArgb)
    val alpha = stroke.opacity
    when (stroke.tool) {
        ColoringTool.Marker -> drawSegmented(stroke, w) { baseColor.copy(alpha = alpha) }

        ColoringTool.Crayon -> {
            drawSegmented(stroke, w * 1.05f) { baseColor.copy(alpha = 0.55f * alpha) }
            drawSegmented(stroke, w * 0.75f) { baseColor.copy(alpha = 0.85f * alpha) }
        }

        ColoringTool.Pencil -> drawSegmented(stroke, w * 0.55f) { baseColor.copy(alpha = 0.7f * alpha) }

        ColoringTool.Watercolor -> {
            drawSegmented(stroke, w * 1.6f) { baseColor.copy(alpha = 0.22f * alpha) }
            drawSegmented(stroke, w * 0.9f) { baseColor.copy(alpha = 0.38f * alpha) }
        }

        ColoringTool.Highlighter -> drawSegmented(
            stroke, w * 1.8f, BlendMode.Multiply
        ) { baseColor.copy(alpha = 0.55f * alpha) }

        ColoringTool.Magic -> drawSegmented(stroke, w) { index ->
            Color(MAGIC_HUES[index % MAGIC_HUES.size]).copy(alpha = alpha)
        }

        ColoringTool.Glitter -> {
            drawSegmented(stroke, w * 0.45f) { baseColor.copy(alpha = 0.35f * alpha) }
            stroke.points.forEachIndexed { i, p ->
                val r = if (i % 2 == 0) w * 0.38f else w * 0.22f
                val c = if (i % 3 == 0) Color.White else baseColor
                drawCircle(color = c.copy(alpha = alpha), radius = r, center = Offset(p.x, p.y))
            }
        }

        ColoringTool.Eraser -> drawSegmented(
            stroke, w * 1.6f, BlendMode.Clear
        ) { Color.Transparent }

        ColoringTool.Fill -> {
            stroke.points.firstOrNull()?.let { p ->
                drawCircle(
                    color = baseColor.copy(alpha = alpha),
                    radius = w * 5.5f,
                    center = Offset(p.x, p.y)
                )
            }
        }

        ColoringTool.Eyedropper, ColoringTool.TextTool -> { /* no drawing */ }
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
            color = colorFor(0), radius = strokeWidth / 2f,
            center = Offset(p.x, p.y), blendMode = blendMode
        )
        return
    }
    for (i in 1 until stroke.points.size) {
        val a = stroke.points[i - 1]
        val b = stroke.points[i]
        drawLine(
            color = colorFor(i),
            start = Offset(a.x, a.y), end = Offset(b.x, b.y),
            strokeWidth = strokeWidth, cap = StrokeCap.Round, blendMode = blendMode
        )
    }
}

private val MAGIC_HUES: List<Long> = listOf(
    0xFFEF5350, 0xFFFFA726, 0xFFFFEB3B, 0xFF66BB6A,
    0xFF26A69A, 0xFF42A5F5, 0xFF7E57C2, 0xFFEC407A
)

internal fun TextFont.toTypeface(): android.graphics.Typeface = when (this) {
    TextFont.Normal -> android.graphics.Typeface.DEFAULT
    TextFont.Bold -> android.graphics.Typeface.DEFAULT_BOLD
    TextFont.Italic -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
    TextFont.BoldItalic -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD_ITALIC)
}

internal fun TextFont.toFontWeight(): androidx.compose.ui.text.font.FontWeight = when (this) {
    TextFont.Bold, TextFont.BoldItalic -> androidx.compose.ui.text.font.FontWeight.Bold
    else -> androidx.compose.ui.text.font.FontWeight.Normal
}

internal fun TextFont.toFontStyle(): androidx.compose.ui.text.font.FontStyle = when (this) {
    TextFont.Italic, TextFont.BoldItalic -> androidx.compose.ui.text.font.FontStyle.Italic
    else -> androidx.compose.ui.text.font.FontStyle.Normal
}
