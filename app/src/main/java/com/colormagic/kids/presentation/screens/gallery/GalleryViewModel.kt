package com.colormagic.kids.presentation.screens.gallery

import androidx.lifecycle.ViewModel
import com.colormagic.kids.domain.model.CategoryIdeas
import com.colormagic.kids.domain.model.GalleryArtwork
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class GalleryUiState(
    /** Source-of-truth list, before filtering. */
    private val allArtworks: List<GalleryArtwork> = sampleArtworks,
    /** null = "All" (no filter). Otherwise a key from [CategoryIdeas]. */
    val selectedCategory: String? = null,
    /** Category keys that have at least one artwork — drives which chips show. */
    val availableCategories: List<String> = CategoryIdeas.keys
) {
    /** Artworks visible after applying the category filter. */
    val artworks: List<GalleryArtwork>
        get() = if (selectedCategory == null) allArtworks
        else allArtworks.filter { it.category == selectedCategory }

    internal fun withArtworks(list: List<GalleryArtwork>): GalleryUiState =
        copy(allArtworks = list)
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    // TODO: inject GalleryRepository when persistence lands.
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    fun onDelete(id: String) {
        _uiState.update { current ->
            current.withArtworks(current.artworks.filterNot { it.id == id })
        }
    }

    /** Apply a category filter — null clears the filter ("All"). */
    fun onCategorySelected(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
}

// Replaced by repository-backed data when wired up. Categories are assigned
// so the filter chip row has something to demonstrate against.
private val sampleArtworks = listOf(
    GalleryArtwork(
        id = "space",
        title = "Space Adventure",
        dateLabel = "Today",
        placeholderTint = 0xFF1A2438,
        category = CategoryIdeas.SPACE
    ),
    GalleryArtwork(
        id = "bob",
        title = "Friendly Bob",
        dateLabel = "Yesterday",
        placeholderTint = 0xFFD7E8C2,
        category = CategoryIdeas.ANIMALS
    ),
    GalleryArtwork(
        id = "tree",
        title = "Rainbow Tree",
        dateLabel = "Last Week",
        placeholderTint = 0xFFE6DCF5,
        category = CategoryIdeas.NATURE
    )
)
