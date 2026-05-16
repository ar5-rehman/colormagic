package com.colormagic.kids.presentation.screens.coloring

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.R
import com.colormagic.kids.domain.model.BrushSize
import com.colormagic.kids.domain.model.ColoringTool
import com.colormagic.kids.domain.model.DefaultPalette
import com.colormagic.kids.domain.model.PaintColor
import com.colormagic.kids.domain.model.Sketch
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
     * Per-pixel mask of where paint is allowed.
     *   Opaque  → fillable paper
     *   Clear   → boundary line (paint is clipped out here)
     * Null while the mask is still being computed on first load.
     */
    val fillableMask: ImageBitmap? = null
) {
    val canUndo: Boolean get() = strokes.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val selectedColor: PaintColor
        get() = palette.firstOrNull { it.id == selectedColorId } ?: palette.first()
}

@HiltViewModel
class ColoringViewModel @Inject constructor(
    @ApplicationContext private val context: Context
    // TODO: inject SketchRepository when persistence lands.
) : ViewModel() {

    private val _uiState = MutableStateFlow(ColoringUiState())
    val uiState: StateFlow<ColoringUiState> = _uiState.asStateFlow()

    init {
        // Compute the fillable mask off the main thread. The kid can still
        // paint while we wait (mask is null → no clipping), and as soon as it
        // arrives, in-progress strokes start respecting boundaries.
        //
        // Backend swap-in: replace R.drawable.sketch with a downloaded bitmap.
        viewModelScope.launch {
            val mask = loadFillableMask(context, R.drawable.sketch)
            _uiState.update { it.copy(fillableMask = mask) }
        }
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

    fun onSave() {
        // TODO: rasterise canvas + mask → persist as SavedPicture.
    }
}

private val stubSketch = Sketch(
    id = "stub",
    prompt = "A friendly forest monster",
    imageUrl = null,
    placeholderTint = 0xFFFFFFFF
)
