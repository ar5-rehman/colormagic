package com.colormagic.kids.presentation.screens.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.data.telemetry.AppTelemetry
import com.colormagic.kids.domain.repository.FeedbackRepository
import com.colormagic.kids.domain.repository.FeedbackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupportUiState(
    val type: FeedbackType = FeedbackType.Suggestion,
    val message: String = "",
    val email: String = "",
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean get() = message.isNotBlank() && !isSubmitting
    val messageCharCount: Int get() = message.length
}

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val telemetry: AppTelemetry
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

    init {
        telemetry.logAppEvent("support_screen_opened")
    }

    fun onTypeSelected(type: FeedbackType) = _uiState.update { it.copy(type = type) }

    fun onMessageChanged(value: String) =
        _uiState.update { it.copy(message = value.take(MAX_MESSAGE), errorMessage = null) }

    fun onEmailChanged(value: String) =
        _uiState.update { it.copy(email = value.take(MAX_EMAIL)) }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            feedbackRepository.submit(
                type = state.type,
                message = state.message,
                email = state.email.ifBlank { null }
            ).onSuccess {
                telemetry.logAppEvent("feedback_submitted")
                _uiState.update { it.copy(isSubmitting = false, isSubmitted = true) }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "We couldn't send that just now. Check your " +
                            "connection, or use \"Email us directly\" below."
                    )
                }
            }
        }
    }

    /** Reset the form after a successful submit so the parent can send more. */
    fun onSendAnother() = _uiState.value.let { _uiState.value = SupportUiState() }

    private companion object {
        const val MAX_MESSAGE = 4000
        const val MAX_EMAIL = 200
    }
}
