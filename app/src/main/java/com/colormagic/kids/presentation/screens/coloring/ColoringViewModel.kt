package com.colormagic.kids.presentation.screens.coloring

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.colormagic.kids.data.gallery.ArtworkRenderer
import com.colormagic.kids.data.local.preferences.ChallengePreferences
import com.colormagic.kids.domain.model.BrushSize
import com.colormagic.kids.domain.model.ColorPalettes
import com.colormagic.kids.domain.model.ColoringTool
import com.colormagic.kids.domain.model.PaintColor
import com.colormagic.kids.domain.model.Sketch
import com.colormagic.kids.domain.repository.GalleryRepository
import com.colormagic.kids.presentation.sketch.SketchSession
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ColoringUiState(
    val sketch: Sketch = stubSketch,
    val selectedPaletteId: String = ColorPalettes.default.id,
    val palette: List<PaintColor> = ColorPalettes.default.colors,
    val selectedColorId: String = ColorPalettes.default.colors.first().id,
    val tool: ColoringTool = ColoringTool.Crayon,
    val brushSize: BrushSize = BrushSize.Medium,
    val strokes: List<Stroke> = emptyList(),
    val redoStack: List<Stroke> = emptyList(),
    val sketchImage: ImageBitmap? = null,
    val fillableMask: ImageBitmap? = null,
    val isSaving: Boolean = false,
    val symmetryEnabled: Boolean = false,
    val colorByNumberEnabled: Boolean = false,
    val colorRegions: List<ColorRegion> = emptyList(),
    val canvasWidthPx: Int = 0,
    val canvasHeightPx: Int = 0,
    val strokeWidthBase: Float = 18f,
    val opacity: Float = 1f,
    val zoomScale: Float = 1f,
    val zoomOffset: Offset = Offset.Zero,
    val showTextDialog: Boolean = false,
    val pendingTextPosition: Offset = Offset.Zero,
    val selectedTextFont: TextFont = TextFont.Normal,
    val draggingTextIndex: Int = -1,
    val isChallenge: Boolean = false,
    val challengeScore: ChallengeScore? = null,
    val showChallengeResult: Boolean = false,
) {
    val canUndo: Boolean get() = strokes.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val selectedColor: PaintColor
        get() = palette.firstOrNull { it.id == selectedColorId } ?: palette.first()
}

@HiltViewModel
class ColoringViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sketchSession: SketchSession,
    private val galleryRepository: GalleryRepository
) : ViewModel() {

    private val challengePrefs by lazy { ChallengePreferences(context) }

    private val _uiState = MutableStateFlow(ColoringUiState())
    val uiState: StateFlow<ColoringUiState> = _uiState.asStateFlow()

    init {
        loadSketch()
    }

    private fun loadSketch() {
        viewModelScope.launch {
            val sketch = sketchSession.currentSketch.value
            if (sketch != null) {
                _uiState.update { it.copy(sketch = sketch, isChallenge = sketchSession.isChallenge) }
            }

            val bitmap = sketch?.imageUrl?.let { url ->
                runCatching { downloadBitmap(url) }.getOrNull()
            }

            if (bitmap != null) {
                _uiState.update {
                    it.copy(
                        sketchImage = bitmap.asImageBitmap(),
                        fillableMask = computeFillableMask(bitmap)
                    )
                }
            }
        }
    }

    private suspend fun downloadBitmap(url: String): android.graphics.Bitmap? {
        val loader = ImageLoader.Builder(context).build()
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        return (result as? SuccessResult)
            ?.let { it.drawable as? BitmapDrawable }
            ?.bitmap
    }

    // ── Color ────────────────────────────────────────────────────────

    fun onColorSelected(id: String) {
        if (id.startsWith("cbn_")) {
            val regionNum = id.removePrefix("cbn_").toIntOrNull() ?: return
            val region = _uiState.value.colorRegions.firstOrNull { it.number == regionNum } ?: return
            val cbnColor = PaintColor(id, "Color $regionNum", region.assignedColor)
            _uiState.update {
                it.copy(palette = it.palette + cbnColor, selectedColorId = id)
            }
            return
        }
        _uiState.update { it.copy(selectedColorId = id) }
    }

    fun onToolSelected(tool: ColoringTool) = _uiState.update { it.copy(tool = tool) }

    fun onPaletteSelected(paletteId: String) {
        val palette = ColorPalettes.byId(paletteId)
        _uiState.update {
            it.copy(
                selectedPaletteId = palette.id,
                palette = palette.colors,
                selectedColorId = palette.colors.first().id
            )
        }
    }

    // ── Brush size (presets + slider) ────────────────────────────────

    fun onBrushSizeSelected(size: BrushSize) = _uiState.update {
        it.copy(brushSize = size, strokeWidthBase = size.baseWidthPx)
    }

    fun onStrokeWidthChanged(width: Float) = _uiState.update {
        it.copy(strokeWidthBase = width)
    }

    // ── Opacity ─────────────────────────────────────────────────────

    fun onOpacityChanged(opacity: Float) = _uiState.update {
        it.copy(opacity = opacity.coerceIn(0.1f, 1f))
    }

    // ── Mode toggles ────────────────────────────────────────────────

    fun onToggleSymmetry() = _uiState.update { it.copy(symmetryEnabled = !it.symmetryEnabled) }

    fun onToggleColorByNumber() {
        val current = _uiState.value
        if (!current.colorByNumberEnabled && current.colorRegions.isEmpty() && current.fillableMask != null) {
            viewModelScope.launch {
                val regions = RegionDetector.detect(current.fillableMask)
                _uiState.update { it.copy(colorByNumberEnabled = true, colorRegions = regions) }
            }
        } else {
            _uiState.update { it.copy(colorByNumberEnabled = !it.colorByNumberEnabled) }
        }
    }

    fun onCanvasWidthChanged(widthPx: Int) {
        if (widthPx > 0) _uiState.update { it.copy(canvasWidthPx = widthPx) }
    }

    fun onCanvasSizeChanged(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            _uiState.update { it.copy(canvasWidthPx = width, canvasHeightPx = height) }
        }
    }

    // ── Zoom & Pan ──────────────────────────────────────────────────

    fun onZoomChanged(scale: Float, offset: Offset) = _uiState.update {
        it.copy(
            zoomScale = scale.coerceIn(1f, 5f),
            zoomOffset = offset
        )
    }

    fun onZoomIn() = _uiState.update {
        it.copy(zoomScale = (it.zoomScale + 0.5f).coerceAtMost(5f))
    }

    fun onZoomOut() = _uiState.update {
        val newScale = (it.zoomScale - 0.5f).coerceAtLeast(1f)
        it.copy(
            zoomScale = newScale,
            zoomOffset = if (newScale <= 1.01f) Offset.Zero else it.zoomOffset
        )
    }

    fun onPanLeft() = pan(-PAN_STEP, 0f)
    fun onPanRight() = pan(PAN_STEP, 0f)
    fun onPanUp() = pan(0f, -PAN_STEP)
    fun onPanDown() = pan(0f, PAN_STEP)

    private fun pan(dx: Float, dy: Float) = _uiState.update {
        if (it.zoomScale <= 1.01f) return@update it
        it.copy(zoomOffset = Offset(it.zoomOffset.x + dx, it.zoomOffset.y + dy))
    }

    fun onResetZoom() = _uiState.update {
        it.copy(zoomScale = 1f, zoomOffset = Offset.Zero)
    }

    companion object {
        private const val PAN_STEP = 80f
    }

    // ── Eyedropper ──────────────────────────────────────────────────

    fun onEyedropperPick(x: Float, y: Float) {
        val state = _uiState.value
        // Search strokes in reverse (top-most first) for the color at (x, y)
        for (stroke in state.strokes.asReversed()) {
            if (stroke.tool == ColoringTool.Eraser) continue
            val w = stroke.effectiveWidthPx(1f) * 2f
            for (p in stroke.points) {
                val dx = p.x - x
                val dy = p.y - y
                if (dx * dx + dy * dy <= w * w) {
                    val picked = PaintColor("picked", "Picked", stroke.colorArgb)
                    _uiState.update {
                        it.copy(
                            palette = it.palette + picked,
                            selectedColorId = "picked",
                            tool = ColoringTool.Crayon
                        )
                    }
                    return
                }
            }
        }
        // Nothing found in strokes — read from sketch image if available
        val img = state.sketchImage
        if (img != null && state.canvasWidthPx > 0 && state.canvasHeightPx > 0) {
            val px = ((x / state.canvasWidthPx) * img.width).toInt().coerceIn(0, img.width - 1)
            val py = ((y / state.canvasHeightPx) * img.height).toInt().coerceIn(0, img.height - 1)
            val buffer = IntArray(1)
            img.readPixels(buffer, startX = px, startY = py, width = 1, height = 1)
            val argb = buffer[0].toLong() and 0xFFFFFFFFL
            if ((argb ushr 24) > 32) {
                val picked = PaintColor("picked", "Picked", argb or (0xFFL shl 24))
                _uiState.update {
                    it.copy(
                        palette = it.palette + picked,
                        selectedColorId = "picked",
                        tool = ColoringTool.Crayon
                    )
                }
            }
        }
    }

    // ── Text Tool ───────────────────────────────────────────────────

    fun onTextButtonClicked() {
        val state = _uiState.value
        val cx = state.canvasWidthPx / 2f
        val cy = state.canvasHeightPx / 2f
        _uiState.update {
            it.copy(tool = ColoringTool.TextTool, showTextDialog = true, pendingTextPosition = Offset(cx, cy))
        }
    }

    fun onTextToolTap(x: Float, y: Float) {
        _uiState.update {
            it.copy(showTextDialog = true, pendingTextPosition = Offset(x, y))
        }
    }

    fun onTextDialogDismiss() = _uiState.update { it.copy(showTextDialog = false) }

    fun onTextFontSelected(font: TextFont) = _uiState.update { it.copy(selectedTextFont = font) }

    fun onTextConfirmed(text: String) {
        if (text.isBlank()) {
            _uiState.update { it.copy(showTextDialog = false) }
            return
        }
        val state = _uiState.value
        val pos = state.pendingTextPosition
        val textStroke = Stroke(
            tool = ColoringTool.TextTool,
            colorArgb = state.selectedColor.argb,
            size = state.brushSize,
            points = listOf(StrokePoint(pos.x, pos.y)),
            strokeWidthBase = state.strokeWidthBase,
            opacity = state.opacity,
            text = text,
            textSizeSp = state.strokeWidthBase.coerceIn(14f, 48f),
            textFont = state.selectedTextFont
        )
        _uiState.update { s ->
            val newStrokes = buildList {
                addAll(s.strokes)
                add(textStroke)
            }
            s.copy(strokes = newStrokes, redoStack = emptyList(), showTextDialog = false)
        }
    }

    // ── Draggable text ──────────────────────────────────────────────

    fun onTextDragStart(x: Float, y: Float): Boolean {
        val state = _uiState.value
        val threshold = 40f * (state.canvasWidthPx.toFloat() / 360f).coerceAtLeast(1f)
        val thresholdSq = threshold * threshold
        for (i in state.strokes.indices.reversed()) {
            val s = state.strokes[i]
            if (s.tool != ColoringTool.TextTool || s.text == null) continue
            val p = s.points.firstOrNull() ?: continue
            val dx = p.x - x
            val dy = p.y - y
            if (dx * dx + dy * dy <= thresholdSq) {
                _uiState.update { it.copy(draggingTextIndex = i) }
                return true
            }
        }
        return false
    }

    fun onTextDragMove(x: Float, y: Float) {
        val idx = _uiState.value.draggingTextIndex
        if (idx < 0) return
        _uiState.update { state ->
            val strokes = state.strokes.toMutableList()
            if (idx < strokes.size) {
                strokes[idx] = strokes[idx].copy(points = listOf(StrokePoint(x, y)))
            }
            state.copy(strokes = strokes)
        }
    }

    fun onTextDragEnd() = _uiState.update { it.copy(draggingTextIndex = -1) }

    // ── Challenge ───────────────────────────────────────────────────

    fun setIsChallenge(isChallenge: Boolean) = _uiState.update { it.copy(isChallenge = isChallenge) }

    fun submitChallenge() {
        val state = _uiState.value
        viewModelScope.launch {
            val score = ChallengeScorer.score(
                fillableMask = state.fillableMask,
                strokes = state.strokes,
                canvasWidth = state.canvasWidthPx,
                canvasHeight = state.canvasHeightPx,
                densityScale = 1f
            )
            challengePrefs.saveResult(score.totalScore, score.stars)
            _uiState.update { it.copy(challengeScore = score, showChallengeResult = true) }
        }
    }

    fun dismissChallengeResult() = _uiState.update { it.copy(showChallengeResult = false) }

    // ── Strokes ─────────────────────────────────────────────────────

    fun onStrokeFinished(stroke: Stroke) {
        if (stroke.points.isEmpty()) return
        val enriched = stroke.copy(
            strokeWidthBase = _uiState.value.strokeWidthBase,
            opacity = _uiState.value.opacity
        )
        _uiState.update { state ->
            val newStrokes = buildList {
                addAll(state.strokes)
                add(enriched)
                if (state.symmetryEnabled && state.canvasWidthPx > 0) {
                    add(enriched.mirroredHorizontally(state.canvasWidthPx.toFloat()))
                }
            }
            state.copy(strokes = newStrokes, redoStack = emptyList())
        }
    }

    private fun Stroke.mirroredHorizontally(canvasWidth: Float): Stroke = copy(
        points = points.map { StrokePoint(canvasWidth - it.x, it.y) }
    )

    fun onUndo() {
        _uiState.update { state ->
            val last = state.strokes.lastOrNull() ?: return@update state
            state.copy(
                strokes = state.strokes.dropLast(1),
                redoStack = state.redoStack + last
            )
        }
    }

    fun onRedo() {
        _uiState.update { state ->
            val last = state.redoStack.lastOrNull() ?: return@update state
            state.copy(
                strokes = state.strokes + last,
                redoStack = state.redoStack.dropLast(1)
            )
        }
    }

    fun onClear() {
        _uiState.update { it.copy(strokes = emptyList(), redoStack = emptyList()) }
    }

    // ── Save ────────────────────────────────────────────────────────

    suspend fun saveArtwork(
        canvasWidthPx: Int,
        canvasHeightPx: Int,
        densityScale: Float
    ): Boolean {
        val state = _uiState.value
        val sketchImage = state.sketchImage ?: return false
        if (canvasWidthPx <= 0 || canvasHeightPx <= 0) return false

        _uiState.update { it.copy(isSaving = true) }
        return try {
            val bitmap = ArtworkRenderer.render(
                sketchImage = sketchImage,
                fillableMask = state.fillableMask,
                strokes = state.strokes,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                densityScale = densityScale
            )
            val saved = galleryRepository.save(
                bitmap = bitmap,
                prompt = state.sketch.prompt,
                category = null
            )
            saved != null
        } finally {
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}

private val stubSketch = Sketch(
    id = "stub",
    prompt = "A friendly forest monster",
    imageUrl = null,
    placeholderTint = 0xFFFFFFFF
)
