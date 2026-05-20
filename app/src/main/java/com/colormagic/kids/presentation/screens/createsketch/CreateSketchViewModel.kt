package com.colormagic.kids.presentation.screens.createsketch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.domain.model.ColoringIdea
import com.colormagic.kids.domain.repository.SketchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateSketchUiState(
    val prompt: String = "",
    val sketchCreditsCost: Int = 1,
    val ideas: List<ColoringIdea> = sampleIdeas,
    /** Total credits left; null until the first quota fetch resolves. */
    val creditsLeft: Int? = null
) {
    /** True only once quota has loaded and there are no credits to spend. */
    val outOfCredits: Boolean get() = creditsLeft == 0

    /** Generation is allowed with a prompt and at least one known credit.
     *  While quota is still loading (null), we don't block — the backend is
     *  the real gate. */
    val canMakeSketch: Boolean get() = prompt.isNotBlank() && !outOfCredits
}

@HiltViewModel
class CreateSketchViewModel @Inject constructor(
    private val sketchRepository: SketchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateSketchUiState())
    val uiState: StateFlow<CreateSketchUiState> = _uiState.asStateFlow()

    /** Pulls the live credit count so "Make My Sketch" can be pre-disabled
     *  when the user is out of credits. The screen calls this on every
     *  resume — a purchase or a sketch made elsewhere changes the number. */
    fun refreshQuota() {
        viewModelScope.launch {
            sketchRepository.getQuota().onSuccess { quota ->
                _uiState.update { it.copy(creditsLeft = quota.totalAvailableCredits) }
            }
        }
    }

    fun onPromptChanged(prompt: String) {
        _uiState.update { it.copy(prompt = prompt) }
    }

    fun onIdeaSelected(idea: ColoringIdea) {
        _uiState.update { it.copy(prompt = idea.title) }
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
