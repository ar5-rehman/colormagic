package com.colormagic.kids.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        // Collect the live credit flow so the pill updates immediately after
        // an ad completes, a purchase finishes, or a sketch is generated —
        // without needing an explicit refreshQuota() call from every screen.
        viewModelScope.launch {
            creditRepository.quotaFlow.collect { quota ->
                _uiState.update {
                    it.copy(
                        sketchesLeft = quota.totalAvailableCredits,
                        isPremium = quota.isPremium
                    )
                }
            }
        }
        // Also do an initial server refresh to pick up any changes that
        // happened while the app was in the background.
        refreshQuota()
    }

    /** Explicit refresh — called on every ON_RESUME from HomeScreen. */
    fun refreshQuota() {
        viewModelScope.launch {
            // refreshQuota() already calls creditRepository.updateLocalQuota()
            // which triggers the quotaFlow collector above.
            sketchRepository.getQuota()
        }
    }
}
