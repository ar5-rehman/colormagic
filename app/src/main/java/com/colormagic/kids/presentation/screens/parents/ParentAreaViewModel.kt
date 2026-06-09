package com.colormagic.kids.presentation.screens.parents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.data.parents.ParentControlsStore
import com.colormagic.kids.domain.model.SketchLimit
import com.colormagic.kids.domain.repository.GalleryRepository
import com.colormagic.kids.domain.repository.SketchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParentAreaUiState(
    /** Human label for the plan card. "…" while quota is loading. */
    val planName: String = "…",
    /** Total credits the kid can spend (free + monthly + extra).
     *  Null until the first quota fetch resolves. */
    val sparkleCredits: Int? = null,
    val sketchLimit: SketchLimit = SketchLimit.Unlimited,
    val allowFreeTextPrompts: Boolean = true,
    /** Per-session screen-time cap in minutes; null = off. */
    val sessionLimitMinutes: Int? = null,
    /** Child's coloring streak — current + best (server-tracked). */
    val streakCurrent: Int = 0,
    val streakBest: Int = 0
)

@HiltViewModel
class ParentAreaViewModel @Inject constructor(
    private val sketchRepository: SketchRepository,
    private val galleryRepository: GalleryRepository,
    private val parentControls: ParentControlsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentAreaUiState())
    val uiState: StateFlow<ParentAreaUiState> = _uiState.asStateFlow()

    init {
        // Mirror persistent parent-control toggles into the UI state. Any
        // change made elsewhere (or persisted from a previous launch) shows
        // up here automatically.
        viewModelScope.launch {
            parentControls.state.collect { controls ->
                _uiState.update {
                    it.copy(
                        sketchLimit = controls.dailyLimit,
                        allowFreeTextPrompts = controls.allowFreeText,
                        sessionLimitMinutes = controls.sessionLimitMinutes
                    )
                }
            }
        }
    }

    /** Pulls the live quota — total credits + plan label. The screen calls
     *  this on every ON_RESUME so a purchase or a fresh sketch reflects
     *  immediately when the parent returns to this screen. */
    fun refreshQuota() {
        viewModelScope.launch {
            sketchRepository.getQuota().onSuccess { quota ->
                _uiState.update {
                    it.copy(
                        sparkleCredits = quota.totalAvailableCredits,
                        planName = labelForPlan(quota.plan, quota.subscriptionActive),
                        streakCurrent = quota.streakCurrent,
                        streakBest = quota.streakBest
                    )
                }
            }
        }
    }

    fun onSketchLimitChanged(limit: SketchLimit) {
        parentControls.setDailyLimit(limit)
    }

    fun onAllowFreeTextPromptsChanged(allow: Boolean) {
        parentControls.setAllowFreeText(allow)
    }

    fun onSessionLimitChanged(minutes: Int?) {
        parentControls.setSessionLimit(minutes)
    }

    /** Wipes every entry in the in-app gallery. The MediaStore PNGs in the
     *  phone gallery are deliberately left alone — once the kid has shared
     *  art into the device library it's theirs to keep. */
    fun onClearArtwork() {
        viewModelScope.launch { galleryRepository.deleteAll() }
    }

    private fun labelForPlan(plan: String, active: Boolean): String = when {
        plan == "pro" && active -> "Magic Pro"
        plan == "pro" -> "Magic Pro (paused)"
        else -> "Free Plan"
    }
}
