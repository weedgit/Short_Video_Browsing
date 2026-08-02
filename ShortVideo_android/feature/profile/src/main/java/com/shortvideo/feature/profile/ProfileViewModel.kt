package com.shortvideo.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.core.DestinationRoute
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

enum class ProfileTab {
    VIDEOS,
    FAVORITES,
    LIKED,
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isTabLoading: Boolean = false,
    val profile: UserProfile? = null,
    val selectedTab: ProfileTab = ProfileTab.VIDEOS,
    val videos: List<ProfileVideoItem> = emptyList(),
    val favorites: List<ProfileVideoItem> = emptyList(),
    val liked: List<ProfileVideoItem> = emptyList(),
    val errorMessage: String? = null,
    val isOtherProfile: Boolean = false,
    val viewerVideoId: String? = null,
    val isMuted: Boolean = false,
) {
    val currentGridItems: List<ProfileVideoItem>
        get() = when (selectedTab) {
            ProfileTab.VIDEOS -> videos
            ProfileTab.FAVORITES -> favorites
            ProfileTab.LIKED -> liked
        }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val socialRepository: SocialRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val targetUserId: String? =
        savedStateHandle.get<String>(DestinationRoute.USER_ID_ARG)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        ProfileUiState(isOtherProfile = targetUserId != null),
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var favoritesLoaded = false
    private var likedLoaded = false

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isOtherProfile = targetUserId != null,
                )
            }
            favoritesLoaded = false
            likedLoaded = false
            runCatching {
                if (targetUserId != null) {
                    val profile = profileRepository.getProfile(targetUserId)
                    val videos = profileRepository.getProfileVideos(targetUserId)
                    profile to videos
                } else {
                    val profile = profileRepository.getMyProfile()
                    val videos = profileRepository.getProfileVideos(profile.id)
                    profile to videos
                }
            }.onSuccess { (profile, videos) ->
                val isOther = targetUserId != null && !profile.isSelf
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        videos = videos,
                        favorites = emptyList(),
                        liked = emptyList(),
                        selectedTab = ProfileTab.VIDEOS,
                        isOtherProfile = isOther,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load profile",
                        profile = if (targetUserId == null) {
                            UserProfile(
                                id = "me",
                                username = "you",
                                displayName = "Your Profile",
                                bio = "Sign in and upload to populate your grid.",
                                isSelf = true,
                            )
                        } else {
                            null
                        },
                        videos = emptyList(),
                        favorites = emptyList(),
                        liked = emptyList(),
                        isOtherProfile = targetUserId != null,
                    )
                }
            }
        }
    }

    fun selectTab(tab: ProfileTab) {
        if (_uiState.value.isOtherProfile) return
        if (_uiState.value.selectedTab == tab) return
        val needsLoad = when (tab) {
            ProfileTab.VIDEOS -> false
            ProfileTab.FAVORITES -> !favoritesLoaded
            ProfileTab.LIKED -> !likedLoaded
        }
        _uiState.update { it.copy(selectedTab = tab, isTabLoading = needsLoad) }
        when (tab) {
            ProfileTab.VIDEOS -> Unit
            ProfileTab.FAVORITES -> loadFavoritesIfNeeded()
            ProfileTab.LIKED -> loadLikedIfNeeded()
        }
    }

    private fun loadFavoritesIfNeeded() {
        if (favoritesLoaded || _uiState.value.isOtherProfile) return
        viewModelScope.launch {
            runCatching { profileRepository.getMySavedVideos() }
                .onSuccess { items ->
                    favoritesLoaded = true
                    _uiState.update { state ->
                        state.copy(
                            favorites = items,
                            isTabLoading = if (state.selectedTab == ProfileTab.FAVORITES) {
                                false
                            } else {
                                state.isTabLoading
                            },
                        )
                    }
                }
                .onFailure {
                    favoritesLoaded = true
                    _uiState.update { state ->
                        state.copy(
                            favorites = emptyList(),
                            isTabLoading = if (state.selectedTab == ProfileTab.FAVORITES) {
                                false
                            } else {
                                state.isTabLoading
                            },
                        )
                    }
                }
        }
    }

    private fun loadLikedIfNeeded() {
        if (likedLoaded || _uiState.value.isOtherProfile) return
        viewModelScope.launch {
            runCatching { profileRepository.getMyLikedVideos() }
                .onSuccess { items ->
                    likedLoaded = true
                    _uiState.update { state ->
                        state.copy(
                            liked = items,
                            isTabLoading = if (state.selectedTab == ProfileTab.LIKED) {
                                false
                            } else {
                                state.isTabLoading
                            },
                        )
                    }
                }
                .onFailure {
                    likedLoaded = true
                    _uiState.update { state ->
                        state.copy(
                            liked = emptyList(),
                            isTabLoading = if (state.selectedTab == ProfileTab.LIKED) {
                                false
                            } else {
                                state.isTabLoading
                            },
                        )
                    }
                }
        }
    }

    fun openVideoViewer(videoId: String) {
        val playable = _uiState.value.currentGridItems.any {
            it.id == videoId && !it.streamUrl.isNullOrBlank()
        }
        if (!playable) return
        _uiState.update { it.copy(viewerVideoId = videoId) }
    }

    fun closeVideoViewer() {
        _uiState.update { it.copy(viewerVideoId = null) }
    }

    fun onToggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
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
                    it.copy(
                        profile = profile.copy(
                            isFollowing = following,
                            followerCount = (profile.followerCount + if (following) 1 else -1)
                                .coerceAtLeast(0),
                        ),
                    )
                }
            }
        }
    }
}
