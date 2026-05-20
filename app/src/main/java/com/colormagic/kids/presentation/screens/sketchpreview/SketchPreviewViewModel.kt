package com.colormagic.kids.presentation.screens.sketchpreview

import androidx.lifecycle.ViewModel
import com.colormagic.kids.domain.model.Sketch
import com.colormagic.kids.presentation.sketch.SketchSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

// Reads the sketch the Loading screen just generated from the shared
// SketchSession. Null only if the screen is reached out of order — the UI
// guards for that.
@HiltViewModel
class SketchPreviewViewModel @Inject constructor(
    sketchSession: SketchSession
) : ViewModel() {
    val sketch: StateFlow<Sketch?> = sketchSession.currentSketch
}
