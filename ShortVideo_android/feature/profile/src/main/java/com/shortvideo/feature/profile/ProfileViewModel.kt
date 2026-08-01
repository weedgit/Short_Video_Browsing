package com.shortvideo.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.domain.model.ProfileVideoItem
import com.shortvideo.domain.model.UserProfile
import com.shortvideo.domain.repository.ProfileRepository
import com.shortvideo.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val socialRepository: SocialRepository,
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
