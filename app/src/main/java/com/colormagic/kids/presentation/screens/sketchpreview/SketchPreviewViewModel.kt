package com.colormagic.kids.presentation.screens.sketchpreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.domain.model.Sketch
import com.colormagic.kids.domain.repository.FeedbackRepository
import com.colormagic.kids.presentation.sketch.SketchSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Reads the sketch the Loading screen just generated from the shared
// SketchSession. Null only if the screen is reached out of order — the UI
// guards for that.
@HiltViewModel
class SketchPreviewViewModel @Inject constructor(
    sketchSession: SketchSession,
    private val feedbackRepository: FeedbackRepository
) : ViewModel() {
    val sketch: StateFlow<Sketch?> = sketchSession.currentSketch

    /** True once this picture has been reported (hides the button). */
    private val _reported = MutableStateFlow(false)
    val reported: StateFlow<Boolean> = _reported.asStateFlow()

    /** One-shot messages for a toast. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun report(sketch: Sketch) {
        if (_reported.value) return
        _reported.value = true
        viewModelScope.launch {
            feedbackRepository.reportImage(sketch.id, sketch.prompt, sketch.imageUrl)
                .onSuccess { _messages.tryEmit("Thanks for telling us. We'll review this picture.") }
                .onFailure {
                    _reported.value = false
                    _messages.tryEmit("Couldn't send the report. Please try again.")
                }
        }
    }
}
