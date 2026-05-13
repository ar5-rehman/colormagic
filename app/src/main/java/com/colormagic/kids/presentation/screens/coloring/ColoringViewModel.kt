package com.colormagic.kids.presentation.screens.coloring

import androidx.lifecycle.ViewModel
import com.colormagic.kids.domain.model.BrushSize
import com.colormagic.kids.domain.model.ColoringTool
import com.colormagic.kids.domain.model.DefaultPalette
import com.colormagic.kids.domain.model.PaintColor
import com.colormagic.kids.domain.model.Sketch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ColoringUiState(
    val sketch: Sketch = stubSketch,
    val palette: List<PaintColor> = DefaultPalette.colors,
    val selectedColorId: String = DefaultPalette.colors.first().id,
    val tool: ColoringTool = ColoringTool.Brush,
    val brushSize: BrushSize = BrushSize.Medium,
    val canUndo: Boolean = false
)

@HiltViewModel
class ColoringViewModel @Inject constructor(
    // TODO: inject SketchRepository, persist strokes, etc.
) : ViewModel() {
    private val _uiState = MutableStateFlow(ColoringUiState())
    val uiState: StateFlow<ColoringUiState> = _uiState.asStateFlow()

    fun onColorSelected(id: String) = _uiState.update { it.copy(selectedColorId = id) }
    fun onToolSelected(tool: ColoringTool) = _uiState.update { it.copy(tool = tool) }
    fun onBrushSizeSelected(size: BrushSize) = _uiState.update { it.copy(brushSize = size) }
    fun onUndo() { /* TODO: pop the last stroke */ }
    fun onClear() { /* TODO: clear all strokes */ }
    fun onSave() {
        // TODO: persist the canvas → return a SavedPicture.id to the caller
    }
}

private val stubSketch = Sketch(
    id = "stub",
    prompt = "A friendly forest monster",
    imageUrl = null,
    placeholderTint = 0xFFF6F4FA
)
