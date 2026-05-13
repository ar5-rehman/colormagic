package com.colormagic.kids.presentation.screens.sketchpreview

import androidx.lifecycle.ViewModel
import com.colormagic.kids.domain.model.Sketch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SketchPreviewUiState(
    val sketch: Sketch = stubSketch
)

@HiltViewModel
class SketchPreviewViewModel @Inject constructor(
    // TODO: inject SketchRepository (and read sketchId from SavedStateHandle)
) : ViewModel() {
    private val _uiState = MutableStateFlow(SketchPreviewUiState())
    val uiState: StateFlow<SketchPreviewUiState> = _uiState.asStateFlow()
}

private val stubSketch = Sketch(
    id = "stub",
    prompt = "A cute dinosaur eating an apple",
    imageUrl = null,
    placeholderTint = 0xFFF7F5FA
)
