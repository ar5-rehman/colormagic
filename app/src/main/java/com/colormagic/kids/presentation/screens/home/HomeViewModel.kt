package com.colormagic.kids.presentation.screens.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class HomeUiState(
    val sketchesLeft: Int = 3,
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
class HomeViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
}
