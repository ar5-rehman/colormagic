package com.colormagic.kids.presentation.screens.loading

import com.colormagic.kids.data.telemetry.AnalyticsEvent
import com.colormagic.kids.data.telemetry.AppTelemetry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.data.parents.ParentControlsStore
import com.colormagic.kids.domain.repository.SketchGenerationResult
import com.colormagic.kids.domain.repository.SketchRepository
import com.colormagic.kids.presentation.sketch.SketchSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// What the Loading screen is currently showing.
sealed interface LoadingUiState {
    /** Backend call in flight. */
    data object Generating : LoadingUiState

    /** Sketch is ready in SketchSession — the screen should navigate on. */
    data object Ready : LoadingUiState

    /** Out of credits — show the "ask a grown-up" message, offer Back. */
    data object NoCredits : LoadingUiState

    /** Recoverable failure (bad prompt or network) — offer Try Again / Back. */
    data class Error(val message: String) : LoadingUiState
}

@HiltViewModel
class LoadingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sketchRepository: SketchRepository,
    private val sketchSession: SketchSession,
    private val parentControls: ParentControlsStore,
    private val telemetry: AppTelemetry
) : ViewModel() {

    private val prompt: String =
        savedStateHandle.get<String>(ARG_PROMPT).orEmpty()
    val isChallenge: Boolean =
        savedStateHandle.get<String>(ARG_IS_CHALLENGE)?.toBooleanStrictOrNull() ?: false

    private val _uiState = MutableStateFlow<LoadingUiState>(LoadingUiState.Generating)
    val uiState: StateFlow<LoadingUiState> = _uiState.asStateFlow()

    init {
        generate()
    }

    /** Kicks off (or retries) the backend generation. */
    fun generate() {
        if (prompt.isBlank()) {
            _uiState.value = LoadingUiState.Error("Please describe a picture first.")
            return
        }
        _uiState.value = LoadingUiState.Generating
        // Never log the prompt itself — only its length and the flow.
        val params = mapOf("is_challenge" to isChallenge, "prompt_length" to prompt.length)
        telemetry.logEvent(AnalyticsEvent.SKETCH_GENERATION_STARTED, params)
        viewModelScope.launch {
            val result = sketchRepository.generateSketch(prompt, isChallenge)
            telemetry.logEvent(
                if (result is SketchGenerationResult.Success) AnalyticsEvent.SKETCH_GENERATED
                else AnalyticsEvent.SKETCH_GENERATION_FAILED,
                params + ("outcome" to when (result) {
                    is SketchGenerationResult.Success -> "success"
                    SketchGenerationResult.NoCredits -> "no_credits"
                    is SketchGenerationResult.Rejected -> "rejected"
                    is SketchGenerationResult.Failed -> "failed"
                })
            )
            _uiState.value = when (result) {
                is SketchGenerationResult.Success -> {
                    // Hand the sketch to the session so SketchPreview / Coloring
                    // can pick it up after navigation.
                    sketchSession.setCurrentSketch(result.sketch, challenge = isChallenge)
                    // Tick today's local sketch counter — drives the
                    // parent-set daily limit gate in CreateSketch.
                    parentControls.recordSketch()
                    LoadingUiState.Ready
                }
                SketchGenerationResult.NoCredits -> LoadingUiState.NoCredits
                is SketchGenerationResult.Rejected -> LoadingUiState.Error(result.message)
                is SketchGenerationResult.Failed -> LoadingUiState.Error(result.message)
            }
        }
    }

    companion object {
        const val ARG_PROMPT = "prompt"
        const val ARG_IS_CHALLENGE = "isChallenge"
    }
}
