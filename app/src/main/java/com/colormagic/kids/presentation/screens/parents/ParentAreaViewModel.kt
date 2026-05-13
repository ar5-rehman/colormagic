package com.colormagic.kids.presentation.screens.parents

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class SketchLimit(val label: String) {
    Five("5"), Ten("10"), Unlimited("∞")
}

data class ParentAreaUiState(
    val planName: String = "Magic Pro",
    val sparkleCredits: Int = 1240,
    val sketchLimit: SketchLimit = SketchLimit.Ten,
    val allowFreeTextPrompts: Boolean = true
)

@HiltViewModel
class ParentAreaViewModel @Inject constructor(
    // No-op constructor — the Parent Gate is mandatory and lives in
    // [ParentsScreen], so this VM doesn't need to know about it.
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentAreaUiState())
    val uiState: StateFlow<ParentAreaUiState> = _uiState.asStateFlow()

    fun onSketchLimitChanged(limit: SketchLimit) =
        _uiState.update { it.copy(sketchLimit = limit) }

    fun onAllowFreeTextPromptsChanged(allow: Boolean) =
        _uiState.update { it.copy(allowFreeTextPrompts = allow) }

    fun onClearArtwork() {
        // TODO: wire to repository.deleteAll()
    }
}
