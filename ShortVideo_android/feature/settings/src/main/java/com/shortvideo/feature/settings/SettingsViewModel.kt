package com.shortvideo.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.domain.model.UserProfile
import com.shortvideo.domain.repository.AuthRepository
import com.shortvideo.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isLoadingProfile: Boolean = true,
    val isSavingProfile: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val profile: UserProfile? = null,
    val displayName: String = "",
    val bio: String = "",
    val errorMessage: String? = null,
    val profileSavedMessage: String? = null,
    val showDeleteConfirm: Boolean = false,
    val accountDeleted: Boolean = false,
    val loggedOut: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProfile = true, errorMessage = null) }
            runCatching { profileRepository.getMyProfile() }
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isLoadingProfile = false,
                            profile = profile,
                            displayName = profile.displayName,
                            bio = profile.bio.orEmpty(),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingProfile = false,
                            errorMessage = error.message ?: "Unable to load profile",
                        )
                    }
                }
        }
    }

    fun onDisplayNameChange(value: String) {
        if (value.length <= 50) {
            _uiState.update { it.copy(displayName = value, profileSavedMessage = null) }
        }
    }

    fun onBioChange(value: String) {
        if (value.length <= 200) {
            _uiState.update { it.copy(bio = value, profileSavedMessage = null) }
        }
    }

    fun saveProfile() {
        val trimmedName = _uiState.value.displayName.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Display name is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSavingProfile = true, errorMessage = null, profileSavedMessage = null)
            }
            runCatching {
                profileRepository.updateMyProfile(
                    displayName = trimmedName,
                    bio = _uiState.value.bio.trim().ifEmpty { null },
                )
            }.onSuccess { profile ->
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        profile = profile,
                        displayName = profile.displayName,
                        bio = profile.bio.orEmpty(),
                        profileSavedMessage = "Profile saved",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        errorMessage = error.message ?: "Unable to save profile",
                    )
                }
            }
        }
    }

    fun onAvatarSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isUploadingAvatar = true, errorMessage = null, profileSavedMessage = null)
            }
            runCatching {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri)?.takeIf { it.startsWith("image/") }
                    ?: "image/jpeg"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Unable to read image")
                if (bytes.isEmpty()) error("Image is empty")
                if (bytes.size > 5 * 1024 * 1024) error("Avatar must be 5 MB or smaller")
                val ext = when (mimeType) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    "image/gif" -> "gif"
                    else -> "jpg"
                }
                profileRepository.uploadAvatar(bytes, mimeType, "avatar.$ext")
            }.onSuccess { profile ->
                _uiState.update {
                    it.copy(
                        isUploadingAvatar = false,
                        profile = profile,
                        profileSavedMessage = "Avatar updated",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isUploadingAvatar = false,
                        errorMessage = error.message ?: "Unable to update avatar",
                    )
                }
            }
        }
    }

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
