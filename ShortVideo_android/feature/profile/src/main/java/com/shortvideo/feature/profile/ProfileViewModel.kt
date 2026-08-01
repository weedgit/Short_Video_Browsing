package com.shortvideo.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.domain.model.ProfileVideoItem
import com.shortvideo.domain.model.UserProfile
import com.shortvideo.domain.repository.ProfileRepository
import com.shortvideo.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val videos: List<ProfileVideoItem> = emptyList(),
    val errorMessage: String? = null,
    val showEditSheet: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val editError: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val socialRepository: SocialRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val profile = profileRepository.getMyProfile()
                val videos = profileRepository.getProfileVideos(profile.id)
                profile to videos
            }.onSuccess { (profile, videos) ->
                _uiState.update {
                    it.copy(isLoading = false, profile = profile, videos = videos)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load profile",
                        profile = UserProfile(
                            id = "me",
                            username = "you",
                            displayName = "Your Profile",
                            bio = "Sign in and upload to populate your grid.",
                            isSelf = true,
                        ),
                        videos = emptyList(),
                    )
                }
            }
        }
    }

    fun openEditProfile() {
        _uiState.update { it.copy(showEditSheet = true, editError = null) }
    }

    fun dismissEditProfile() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(showEditSheet = false, editError = null) }
    }

    fun saveProfile(displayName: String, bio: String) {
        val trimmedName = displayName.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(editError = "Display name is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, editError = null) }
            runCatching {
                profileRepository.updateMyProfile(
                    displayName = trimmedName,
                    bio = bio.trim().ifEmpty { null },
                )
            }.onSuccess { profile ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        showEditSheet = false,
                        profile = profile,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        editError = error.message ?: "Unable to save profile",
                    )
                }
            }
        }
    }

    fun onAvatarSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, editError = null) }
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
                    it.copy(isUploadingAvatar = false, profile = profile)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isUploadingAvatar = false,
                        editError = error.message ?: "Unable to update avatar",
                    )
                }
            }
        }
    }

    fun clearEditError() {
        _uiState.update { it.copy(editError = null) }
    }

    fun toggleFollow() {
        val profile = _uiState.value.profile ?: return
        if (profile.isSelf) return
        viewModelScope.launch {
            runCatching {
                if (profile.isFollowing) socialRepository.unfollowUser(profile.id)
                else socialRepository.followUser(profile.id)
            }.onSuccess { following ->
                _uiState.update {
                    it.copy(profile = profile.copy(isFollowing = following))
                }
            }
        }
    }
}
