package com.colormagic.kids.presentation.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.domain.model.CategoryIdeas
import com.colormagic.kids.domain.model.GalleryArtwork
import com.colormagic.kids.domain.repository.GalleryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GalleryUiState(
    /** Source-of-truth list, before filtering. */
    private val allArtworks: List<GalleryArtwork> = emptyList(),
    /** null = "All" (no filter). Otherwise a key from [CategoryIdeas]. */
    val selectedCategory: String? = null,
    /** Category keys that have at least one artwork — drives which chips show. */
    val availableCategories: List<String> = CategoryIdeas.keys
) {
    /** Artworks visible after applying the category filter. */
    val artworks: List<GalleryArtwork>
        get() = if (selectedCategory == null) allArtworks
        else allArtworks.filter { it.category == selectedCategory }
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val galleryRepository: GalleryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        // Subscribe to the persistent gallery store. Every new save flows
        // through here automatically, so the screen reflects them without
        // a manual refresh.
        viewModelScope.launch {
            galleryRepository.artworks.collect { list ->
                _uiState.update { it.copy(allArtworks = list) }
            }
        }
    }

    fun onDelete(id: String) {
        viewModelScope.launch { galleryRepository.delete(id) }
    }

    /** Apply a category filter — null clears the filter ("All"). */
    fun onCategorySelected(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
}
