package com.colormagic.kids.presentation.screens.savesuccess

import androidx.lifecycle.ViewModel
import com.colormagic.kids.domain.model.SavedPicture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SaveSuccessUiState(
    val picture: SavedPicture = stubPicture
)

@HiltViewModel
class SaveSuccessViewModel @Inject constructor(
    // TODO: inject SavedPictureRepository to look up the picture by id.
) : ViewModel() {
    private val _uiState = MutableStateFlow(SaveSuccessUiState())
    val uiState: StateFlow<SaveSuccessUiState> = _uiState.asStateFlow()
}

private val stubPicture = SavedPicture(
    id = "stub",
    sourceSketchId = "stub",
    imageUrl = null,
    placeholderTint = 0xFFD7E8C2
)
