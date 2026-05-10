package com.colormagic.kids.presentation.screens.createsketch

import androidx.lifecycle.ViewModel
import com.colormagic.kids.domain.model.ColoringIdea
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class CreateSketchUiState(
    val prompt: String = "",
    val sketchCreditsCost: Int = 1,
    val ideas: List<ColoringIdea> = sampleIdeas
)

@HiltViewModel
class CreateSketchViewModel @Inject constructor(
    // TODO: inject IdeaRepository / SketchRepository when backend is wired up.
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateSketchUiState())
    val uiState: StateFlow<CreateSketchUiState> = _uiState.asStateFlow()

    fun onPromptChanged(prompt: String) {
        _uiState.update { it.copy(prompt = prompt) }
    }

    fun onIdeaSelected(idea: ColoringIdea) {
        _uiState.update { it.copy(prompt = idea.title) }
    }

    fun onMakeSketch() {
        // TODO: kick off generation — repository call + navigation event.
    }
}

// Placeholder ideas used until the ideas endpoint is live. Backend will
// replace this list via the (future) IdeaRepository.
private val sampleIdeas = listOf(
    ColoringIdea(
        id = "rocket",
        title = "A happy rocket ship flying past smiling planets",
        previewTint = 0xFFE3F2FD
    ),
    ColoringIdea(
        id = "frog",
        title = "A friendly frog sitting on a giant lily pad",
        previewTint = 0xFFE8F5E9
    ),
    ColoringIdea(
        id = "unicorn",
        title = "A fluffy unicorn eating a big strawberry cupcake",
        previewTint = 0xFFFCE4EC
    )
)
