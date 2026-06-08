package com.colormagic.kids.presentation.screens.createsketch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.data.parents.ParentControlsStore
import com.colormagic.kids.domain.model.CategoryIdeas
import com.colormagic.kids.domain.model.ColoringIdea
import com.colormagic.kids.domain.model.SketchLimit
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
    val creditsLeft: Int? = null,
    /** Whether the parent is subscribed — drives the premium-aware low-credits
     *  popup (no ads / no upsell for premium). */
    val isPremium: Boolean = false,
    /** Parent-set: when false the prompt input is read-only and the kid
     *  can only generate by tapping a category chip. */
    val allowFreeText: Boolean = true,
    /** Parent-set daily cap + how many sketches the kid has used today. */
    val dailyLimit: SketchLimit = SketchLimit.Unlimited,
    val sketchesToday: Int = 0,
    /** True when the user tapped "Make My Sketch" but had no credits. */
    val showLowCreditsModal: Boolean = false
) {
    /** True only once quota has loaded and there are no credits to spend. */
    val outOfCredits: Boolean get() = creditsLeft == 0

    /** True when the parent's local per-day cap has been hit. Unlimited
     *  never blocks; Firebase credit limits are a separate gate. */
    val dailyLimitReached: Boolean
        get() = dailyLimit.perDay?.let { sketchesToday >= it } ?: false

    /** Generation is allowed with a prompt, with at least one known credit,
     *  and within the parent-set daily limit. While quota is still loading
     *  (null), we don't block — the backend is the real credit gate. */
    val canMakeSketch: Boolean
        get() = prompt.isNotBlank() && !outOfCredits && !dailyLimitReached
}

@HiltViewModel
class CreateSketchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sketchRepository: SketchRepository,
    private val parentControls: ParentControlsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateSketchUiState(ideas = freshIdeas()))
    val uiState: StateFlow<CreateSketchUiState> = _uiState.asStateFlow()

    init {
        // Deep-link from Home → category card prefills the prompt with a
        // random idea from that category. Honoured even when free-text is
        // disabled — picking from a chip is the only way to start, and the
        // category route IS a chip-equivalent action.
        val initialCategory: String? = savedStateHandle[Screen.CreateSketch.ARG_CATEGORY]
        if (!initialCategory.isNullOrBlank()) {
            CategoryIdeas.randomIdeaFor(initialCategory)?.let { idea ->
                _uiState.update { it.copy(prompt = idea) }
            }
        }

        // Mirror parent-control toggles into the screen state so:
        //   • the prompt input flips read-only on "Allow free text" off,
        //   • "Make My Sketch" disables itself when the daily cap is hit.
        viewModelScope.launch {
            parentControls.state.collect { controls ->
                _uiState.update {
                    it.copy(
                        allowFreeText = controls.allowFreeText,
                        dailyLimit = controls.dailyLimit,
                        sketchesToday = controls.sketchesToday
                    )
                }
            }
        }
    }

    /** Pulls the live credit count so "Make My Sketch" can be pre-disabled
     *  when the user is out of credits. The screen calls this on every
     *  resume — a purchase or a sketch made elsewhere changes the number. */
    fun refreshQuota() {
        viewModelScope.launch {
            sketchRepository.getQuota().onSuccess { quota ->
                _uiState.update {
                    it.copy(
                        creditsLeft = quota.totalAvailableCredits,
                        isPremium = quota.isPremium
                    )
                }
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
        // Silently drop typing when free-text is disabled — the input itself
        // is read-only, but this is a belt-and-braces guard.
        if (!_uiState.value.allowFreeText) return
        _uiState.update { it.copy(prompt = prompt) }
    }

    fun onIdeaSelected(idea: ColoringIdea) {
        _uiState.update { it.copy(prompt = idea.title) }
    }

    /** Called when the user taps "Make My Sketch" but is out of credits. */
    fun onMakeSketchBlockedByCredits() {
        _uiState.update { it.copy(showLowCreditsModal = true) }
    }

    fun onLowCreditsModalDismissed() {
        _uiState.update { it.copy(showLowCreditsModal = false) }
    }
}

/** Picks 10 random prompts (from a pool of 60+), each carrying its category's
 *  emoji + pastel color, to populate the colorful "Need ideas?" cards. */
private fun freshIdeas(): List<ColoringIdea> =
    CategoryIdeas.allIdeaItems.shuffled().take(10).mapIndexed { i, item ->
        ColoringIdea(
            id = "idea-$i-${item.text.hashCode()}",
            title = item.text,
            emoji = CategoryIdeas.emoji[item.category] ?: "✨",
            previewTint = CategoryIdeas.tint[item.category] ?: 0xFFEDE7F6
        )
    }
