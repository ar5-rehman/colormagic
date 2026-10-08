package com.colormagic.kids.presentation.screens.savesuccess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.domain.model.SavedPicture
import com.colormagic.kids.domain.repository.GalleryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaveSuccessUiState(
    val picture: SavedPicture = stubPicture
)

@HiltViewModel
class SaveSuccessViewModel @Inject constructor(
    galleryRepository: GalleryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SaveSuccessUiState())
    val uiState: StateFlow<SaveSuccessUiState> = _uiState.asStateFlow()

    init {
        // This screen opens right after a save, and the gallery keeps the
        // newest artwork first — so that's the picture to show off.
        viewModelScope.launch {
            val latest = galleryRepository.artworks.first().firstOrNull() ?: return@launch
            _uiState.update {
                it.copy(
                    picture = SavedPicture(
                        id = latest.id,
                        sourceSketchId = latest.id,
                        imageUrl = latest.localUri
                    )
                )
            }
        }
    }
}

private val stubPicture = SavedPicture(
    id = "stub",
    sourceSketchId = "stub",
    imageUrl = null,
    placeholderTint = 0xFFD7E8C2
)
