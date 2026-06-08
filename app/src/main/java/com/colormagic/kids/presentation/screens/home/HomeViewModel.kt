package com.colormagic.kids.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.domain.model.UserQuota
import com.colormagic.kids.domain.repository.CreditRepository
import com.colormagic.kids.domain.repository.SketchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    /** Total credits left; null until the first quota fetch resolves. */
    val sketchesLeft: Int? = null,
    val isPremium: Boolean = false,
    val categories: List<HomeCategory> = HomeCategory.defaults()
)

enum class CategoryTone { Blue, Lavender, Grey, GreenDeep, GreenLight }

data class HomeCategory(
    val id: String,
    val label: String,
    val tone: CategoryTone
) {
    companion object {
        fun defaults(): List<HomeCategory> = listOf(
            HomeCategory("animals", "Animals", CategoryTone.Blue),
            HomeCategory("space", "Space", CategoryTone.GreenDeep),
            HomeCategory("dinosaurs", "Dinosaurs", CategoryTone.Lavender),
            HomeCategory("robots", "Robots", CategoryTone.Blue),
            HomeCategory("princess", "Princess", CategoryTone.GreenLight),
            HomeCategory("nature", "Nature", CategoryTone.Grey)
        )
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sketchRepository: SketchRepository,
    private val creditRepository: CreditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Latest quota observed from the flow (UNKNOWN until one arrives). */
    private var latestQuota: UserQuota = UserQuota.UNKNOWN

    /** True once the first load attempt has finished (success OR failure) or a
     *  real value has arrived. Drives whether the credit pill shimmers.
     *
     *  We can't infer "loaded" from the UNKNOWN sentinel: with FREE_DAILY_CREDITS = 0,
     *  a fresh guest's quota (0 credits) is value-equal to UNKNOWN, so StateFlow
     *  dedupes it and the collector never fires — which previously left the pill
     *  shimmering forever even on a successful fetch. */
    private var creditsResolved = false

    init {
        // Collect the live credit flow so the pill updates immediately after
        // an ad completes, a purchase finishes, or a sketch is generated —
        // without needing an explicit refreshQuota() call from every screen.
        viewModelScope.launch {
            creditRepository.quotaFlow.collect { quota ->
                latestQuota = quota
                if (quota !== UserQuota.UNKNOWN) creditsResolved = true
                publishCredits()
            }
        }
        // Show the last cached balance instantly for returning users, then do
        // an initial server refresh to pick up any background changes.
        viewModelScope.launch {
            creditRepository.seedFromCacheIfUnknown()
            loadQuota()
        }
    }

    /** Explicit refresh — called on every ON_RESUME from HomeScreen. */
    fun refreshQuota() {
        viewModelScope.launch { loadQuota() }
    }

    private suspend fun loadQuota() {
        // getQuota() updates the quotaFlow on success (triggering the collector).
        // Mark resolved afterwards so the shimmer always stops — even when the
        // fetch fails, or succeeds with a 0-credit quota that StateFlow dedupes.
        sketchRepository.getQuota()
        creditsResolved = true
        publishCredits()
    }

    private fun publishCredits() {
        _uiState.update {
            it.copy(
                sketchesLeft = if (creditsResolved) latestQuota.totalAvailableCredits else null,
                isPremium = latestQuota.isPremium
            )
        }
    }
}
