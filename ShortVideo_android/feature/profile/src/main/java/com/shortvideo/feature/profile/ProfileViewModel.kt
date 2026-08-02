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
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var favoritesLoaded = false
    private var likedLoaded = false

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            favoritesLoaded = false
            likedLoaded = false
            runCatching {
                val profile = profileRepository.getMyProfile()
                val videos = profileRepository.getProfileVideos(profile.id)
                profile to videos
            }.onSuccess { (profile, videos) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        videos = videos,
                        favorites = emptyList(),
                        liked = emptyList(),
                        selectedTab = ProfileTab.VIDEOS,
                    )
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
                        favorites = emptyList(),
                        liked = emptyList(),
                    )
                }
            }
        }
    }

    fun selectTab(tab: ProfileTab) {
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
        if (favoritesLoaded) return
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
        if (likedLoaded) return
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
