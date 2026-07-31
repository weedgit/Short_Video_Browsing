package com.shortvideo.feature.settings

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

data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteConfirm: Boolean = false,
    val accountDeleted: Boolean = false,
    val loggedOut: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun showDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = true, errorMessage = null) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.logout()
            _uiState.update { it.copy(isLoading = false, loggedOut = true) }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, showDeleteConfirm = false) }
            authRepository.deleteAccount()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, accountDeleted = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Account deletion failed.",
                        )
                    }
                }
        }
    }

    fun clearNavigationEvents() {
        _uiState.update { it.copy(loggedOut = false, accountDeleted = false) }
    }
}
