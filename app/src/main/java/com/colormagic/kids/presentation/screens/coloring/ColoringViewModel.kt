package com.colormagic.kids.presentation.screens.coloring

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.colormagic.kids.data.gallery.ArtworkRenderer
import com.colormagic.kids.domain.model.BrushSize
import com.colormagic.kids.domain.model.ColoringTool
import com.colormagic.kids.domain.model.DefaultPalette
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
    val palette: List<PaintColor> = DefaultPalette.colors,
    val selectedColorId: String = DefaultPalette.colors.first().id,
    val tool: ColoringTool = ColoringTool.Crayon,
    val brushSize: BrushSize = BrushSize.Medium,
    val strokes: List<Stroke> = emptyList(),
    val redoStack: List<Stroke> = emptyList(),
    /**
     * The line-art the kid is colouring. Null until the backend sketch has
     * downloaded — the canvas falls back to the bundled sample meanwhile.
     */
    val sketchImage: ImageBitmap? = null,
    /**
     * Per-pixel mask of where paint is allowed.
     *   Opaque  → fillable paper
     *   Clear   → boundary line (paint is clipped out here)
     * Null while the mask is still being computed on first load.
     */
    val fillableMask: ImageBitmap? = null,
    /** A save is in progress (render + MediaStore write). */
    val isSaving: Boolean = false
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

    private val _uiState = MutableStateFlow(ColoringUiState())
    val uiState: StateFlow<ColoringUiState> = _uiState.asStateFlow()

    init {
        loadSketch()
    }

    /**
     * Loads the line-art the kid will colour, then computes its fillable mask
     * off the main thread. The kid can paint freely while the mask resolves
     * (mask null → no clipping); strokes start respecting boundaries the
     * moment it lands.
     *
     *  • Real flow → download the backend sketch from its image URL.
     *  • Fallback  → the bundled sample sketch (e.g. opened out of order, or
     *               the download failed).
     */
    private fun loadSketch() {
        viewModelScope.launch {
            val sketch = sketchSession.currentSketch.value
            if (sketch != null) {
                _uiState.update { it.copy(sketch = sketch) }
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
            // No bitmap → leave both null so the canvas keeps shimmering
            // rather than swapping in a misleading bundled drawable. A
            // proper "offline / failed fetch" fallback that rotates through
            // sample sketches ships when the sample library is added.
        }
    }

    /**
     * Downloads the sketch image via Coil. `allowHardware(false)` is required:
     * computeFillableMask reads pixels, which a hardware bitmap forbids.
     */
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

    fun onColorSelected(id: String) = _uiState.update { it.copy(selectedColorId = id) }
    fun onToolSelected(tool: ColoringTool) = _uiState.update { it.copy(tool = tool) }
    fun onBrushSizeSelected(size: BrushSize) = _uiState.update { it.copy(brushSize = size) }

    fun onStrokeFinished(stroke: Stroke) {
        if (stroke.points.isEmpty()) return
        _uiState.update {
            it.copy(
                strokes = it.strokes + stroke,
                redoStack = emptyList()
            )
        }
    }

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

    /**
     * Render the current canvas into a PNG and persist it.
     *
     * Returns true if the save reached MediaStore + the in-app gallery store,
     * false otherwise (no sketch loaded, render failure, MediaStore rejected
     * the write, etc.). The screen uses the result to decide whether to
     * navigate to SaveSuccess.
     *
     * [canvasWidthPx]/[canvasHeightPx] come from the rendered SketchCanvas's
     * actual on-screen size — that's the coordinate space the strokes were
     * captured in, so re-rendering at that size keeps the lines perfectly
     * aligned with the line-art.
     */
    suspend fun saveArtwork(canvasWidthPx: Int, canvasHeightPx: Int): Boolean {
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
                canvasHeightPx = canvasHeightPx
            )
            val saved = galleryRepository.save(
                bitmap = bitmap,
                prompt = state.sketch.prompt,
                category = null  // backend doesn't tag sketches with a category yet
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
