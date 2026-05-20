package com.colormagic.kids.presentation.screens.createsketch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.domain.model.CategoryIdeas
import com.colormagic.kids.domain.model.ColoringIdea
import com.colormagic.kids.domain.repository.SketchRepository
import com.colormagic.kids.presentation.navigation.Screen
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
    /** "Need ideas?" pool — reshuffled every time the screen resumes. */
    val ideas: List<ColoringIdea> = emptyList(),
    /** Stable list of category keys shown as the chip row. */
    val categories: List<String> = CategoryIdeas.keys,
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
    savedStateHandle: SavedStateHandle,
    private val sketchRepository: SketchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateSketchUiState(ideas = freshIdeas()))
    val uiState: StateFlow<CreateSketchUiState> = _uiState.asStateFlow()

    init {
        // Deep-link from Home → category card prefills the prompt with a
        // random idea from that category.
        val initialCategory: String? = savedStateHandle[Screen.CreateSketch.ARG_CATEGORY]
        if (!initialCategory.isNullOrBlank()) {
            CategoryIdeas.randomIdeaFor(initialCategory)?.let { idea ->
                _uiState.update { it.copy(prompt = idea) }
            }
        }
    }

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

    /** Reshuffle the "Need ideas?" pool. Called on every resume so the kid
     *  sees fresh suggestions each time the screen opens. */
    fun shuffleIdeas() {
        _uiState.update { it.copy(ideas = freshIdeas()) }
    }

    /** A category chip was tapped — drop a random prompt from that category
     *  into the input box. The kid can tweak it before submitting. */
    fun onCategorySelected(category: String) {
        val idea = CategoryIdeas.randomIdeaFor(category) ?: return
        _uiState.update { it.copy(prompt = idea) }
    }

    fun onPromptChanged(prompt: String) {
        _uiState.update { it.copy(prompt = prompt) }
    }

    fun onIdeaSelected(idea: ColoringIdea) {
        _uiState.update { it.copy(prompt = idea.title) }
    }
}

/** Picks 6 random prompts from the master pool, each tinted with a kid-
 *  friendly pastel background, to populate the "Need ideas?" cards. */
private fun freshIdeas(): List<ColoringIdea> {
    val tints = listOf(
        0xFFE3F2FD, 0xFFE8F5E9, 0xFFFCE4EC,
        0xFFFFF3E0, 0xFFEDE7F6, 0xFFE0F7FA
    )
    return CategoryIdeas.allIdeas.shuffled().take(6).mapIndexed { i, title ->
        ColoringIdea(
            id = "idea-$i-${title.hashCode()}",
            title = title,
            previewTint = tints[i % tints.size]
        )
    }
}
