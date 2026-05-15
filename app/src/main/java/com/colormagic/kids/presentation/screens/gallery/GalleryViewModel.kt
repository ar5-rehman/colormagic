package com.colormagic.kids.presentation.screens.gallery

import androidx.lifecycle.ViewModel
import com.colormagic.kids.domain.model.GalleryArtwork
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class GalleryUiState(
    val artworks: List<GalleryArtwork> = sampleArtworks
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    // TODO: inject GalleryRepository when persistence lands.
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    fun onDelete(id: String) {
        _uiState.update { current ->
            current.copy(artworks = current.artworks.filterNot { it.id == id })
        }
    }
}

// Replaced by repository-backed data when wired up.
private val sampleArtworks = listOf(
    GalleryArtwork(
        id = "space",
        title = "Space Adventure",
        dateLabel = "Today",
        placeholderTint = 0xFF1A2438
    ),
    GalleryArtwork(
        id = "bob",
        title = "Friendly Bob",
        dateLabel = "Yesterday",
        placeholderTint = 0xFFD7E8C2
    ),
    GalleryArtwork(
        id = "tree",
        title = "Rainbow Tree",
        dateLabel = "Last Week",
        placeholderTint = 0xFFE6DCF5
    )
)
