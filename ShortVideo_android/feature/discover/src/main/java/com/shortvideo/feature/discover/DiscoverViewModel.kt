package com.shortvideo.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.domain.model.DiscoverHashtag
import com.shortvideo.domain.model.DiscoverTab
import com.shortvideo.domain.model.DiscoverVideo
import com.shortvideo.domain.model.UserProfile
import com.shortvideo.domain.repository.DiscoverRepository
import com.shortvideo.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val query: String = "",
    val selectedTab: DiscoverTab = DiscoverTab.VIDEOS,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val hashtags: List<DiscoverHashtag> = emptyList(),
    val users: List<UserProfile> = emptyList(),
    val videos: List<DiscoverVideo> = emptyList(),
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        search()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            search()
        }
    }

    fun onTabSelected(tab: DiscoverTab) {
        if (tab == _uiState.value.selectedTab) return
        _uiState.update { it.copy(selectedTab = tab) }
        search()
    }

    fun onFollowToggle(user: UserProfile) {
        viewModelScope.launch {
            runCatching {
                if (user.isFollowing) socialRepository.unfollowUser(user.id)
                else socialRepository.followUser(user.id)
            }.onSuccess { following ->
                _uiState.update { state ->
                    when (state.selectedTab) {
                        DiscoverTab.FRIENDS -> {
                            if (following) {
                                state.copy(
                                    users = state.users.map {
                                        if (it.id == user.id) it.copy(isFollowing = true) else it
                                    },
                                )
                            } else {
                                state.copy(users = state.users.filterNot { it.id == user.id })
                            }
                        }
                        else -> state.copy(
                            users = state.users.map {
                                if (it.id == user.id) it.copy(isFollowing = following) else it
                            },
                        )
                    }
                }
            }
        }
    }

    private fun search() {
        val query = _uiState.value.query.ifBlank { null }
        val tab = _uiState.value.selectedTab
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { discoverRepository.search(query, tab) }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hashtags = result.hashtags,
                            users = result.users,
                            videos = result.videos,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hashtags = emptyList(),
                            users = emptyList(),
                            videos = emptyList(),
                            errorMessage = error.message ?: "Could not load discover results",
                        )
                    }
                }
        }
    }
}
