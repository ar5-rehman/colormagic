package com.colormagic.kids.presentation.screens.parents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.data.parents.ParentControlsStore
import com.colormagic.kids.domain.model.SketchLimit
import com.colormagic.kids.data.local.preferences.RewardsPreferences
import com.colormagic.kids.domain.repository.AuthRepository
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
    /** Today's daily credits remaining (subset of total). */
    val dailyCreditsLeft: Int = 0,
    val sketchLimit: SketchLimit = SketchLimit.Unlimited,
    val allowFreeTextPrompts: Boolean = true,
    /** Per-session screen-time cap in minutes; null = off. */
    val sessionLimitMinutes: Int? = null,
    /** Child's coloring streak — current + best (server-tracked). */
    val streakCurrent: Int = 0,
    val streakBest: Int = 0,
    /** True while the account deletion request is running. */
    val isDeletingAccount: Boolean = false,
    /** One-shot result message for account deletion (shown as a toast). */
    val accountMessage: String? = null,
    /** If a deleted account on this phone can still be restored: its
     *  permanent-deletion time (epoch millis). */
    val restorableAccountUntil: Long? = null,
    val isRestoringAccount: Boolean = false
)

@HiltViewModel
class ParentAreaViewModel @Inject constructor(
    private val sketchRepository: SketchRepository,
    private val galleryRepository: GalleryRepository,
    private val parentControls: ParentControlsStore,
    private val authRepository: AuthRepository,
    private val rewardsPreferences: RewardsPreferences
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
                        dailyCreditsLeft = quota.remainingFreeSketches,
                        planName = labelForPlan(quota.plan, quota.subscriptionActive),
                        streakCurrent = quota.streakCurrent,
                        streakBest = quota.streakBest,
                        restorableAccountUntil = quota.restorableAccountUntil
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

    /**
     * Deletes the account (restorable for 60 days, then erased) and wipes this
     * device's in-app data (gallery list, badges/progress). Pictures already
     * saved to the phone's photo gallery are left alone. The app continues on
     * a new empty account.
     */
    fun onDeleteAccount() {
        if (_uiState.value.isDeletingAccount) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }
            authRepository.deleteAccount()
                .onSuccess {
                    galleryRepository.deleteAll()
                    rewardsPreferences.clear()
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            accountMessage = "Account deleted. You can restore it from here " +
                                "for the next 60 days."
                        )
                    }
                    refreshQuota()
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            accountMessage = "Couldn't delete your account. Check your connection and try again."
                        )
                    }
                }
        }
    }

    /** Brings back the account deleted on this phone (within 60 days). */
    fun onRestoreAccount() {
        if (_uiState.value.isRestoringAccount) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoringAccount = true) }
            authRepository.restoreDeletedAccount()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isRestoringAccount = false,
                            restorableAccountUntil = null,
                            accountMessage = "Welcome back! Your account has been restored."
                        )
                    }
                    refreshQuota()
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isRestoringAccount = false,
                            accountMessage = "Couldn't restore the account. Please try again."
                        )
                    }
                }
        }
    }

    fun accountMessageShown() {
        _uiState.update { it.copy(accountMessage = null) }
    }

    private fun labelForPlan(plan: String, active: Boolean): String = when {
        plan == "pro" && active -> "Magic Pro"
        plan == "pro" -> "Magic Pro (paused)"
        else -> "Free Plan"
    }
}
