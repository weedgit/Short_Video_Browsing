package com.shortvideo.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.domain.model.DiscoverHashtag
import com.shortvideo.domain.model.FeedVideo
import com.shortvideo.domain.model.UserProfile
import com.shortvideo.domain.repository.DiscoverRepository
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
    val isLoading: Boolean = true,
    val hashtags: List<DiscoverHashtag> = emptyList(),
    val users: List<UserProfile> = emptyList(),
    val videos: List<FeedVideo> = emptyList(),
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        search(null)
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            search(query.ifBlank { null })
        }
    }

    private fun search(query: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { discoverRepository.search(query) }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hashtags = result.hashtags,
                            users = result.users,
                            videos = result.videos,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }
}
