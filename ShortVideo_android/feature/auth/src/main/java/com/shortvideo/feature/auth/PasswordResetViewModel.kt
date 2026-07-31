package com.shortvideo.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PasswordResetUiState(
    val email: String = "",
    val resetToken: String = "",
    val newPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isCompleted: Boolean = false,
)

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PasswordResetUiState())
    val uiState: StateFlow<PasswordResetUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onResetTokenChanged(value: String) {
        _uiState.update { it.copy(resetToken = value, errorMessage = null) }
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }
    }

    fun requestResetToken() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email is required.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            authRepository.requestPasswordReset(email)
                .onSuccess { resetToken ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            resetToken = resetToken.orEmpty(),
                            infoMessage = if (resetToken.isNullOrBlank()) {
                                "If the account exists, reset instructions were sent."
                            } else {
                                "Dev reset token received. Confirm your new password below."
                            },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Reset request failed.",
                        )
                    }
                }
        }
    }

    fun confirmReset() {
        val state = _uiState.value
        if (state.resetToken.isBlank() || state.newPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Reset token and new password are required.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.confirmPasswordReset(state.resetToken, state.newPassword)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isCompleted = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Password reset failed.",
                        )
                    }
                }
        }
    }

    fun clearCompleted() {
        _uiState.update { it.copy(isCompleted = false) }
    }
}
