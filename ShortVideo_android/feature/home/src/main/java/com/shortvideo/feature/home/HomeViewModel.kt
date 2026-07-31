package com.shortvideo.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.domain.model.FeedVideo
import com.shortvideo.domain.model.PlaybackEvent
import com.shortvideo.domain.repository.FeedPlaybackRepository
import com.shortvideo.domain.repository.FeedRepository
import com.shortvideo.domain.repository.PlaybackEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val videos: List<FeedVideo> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val isMuted: Boolean = false,
    val resumePositionsMs: Map<String, Long> = emptyMap(),
    val errorMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val playbackEventRepository: PlaybackEventRepository,
    private val feedPlaybackRepository: FeedPlaybackRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var nextCursor: String? = null
    private var isLoadingMore = false

    init {
        viewModelScope.launch {
            val muted = feedPlaybackRepository.isMuted()
            _uiState.update { it.copy(isMuted = muted) }
            loadInitialFeed()
        }
    }

    fun onNearEnd(currentIndex: Int) {
        val state = _uiState.value
        if (!state.hasMore || isLoadingMore) return
        if (currentIndex < state.videos.lastIndex - 2) return
        loadMoreFeed()
    }

    fun onActiveVideoChanged(video: FeedVideo, positionMs: Long) {
        viewModelScope.launch {
            feedPlaybackRepository.setResumePositionMs(video.id, positionMs)
            playbackEventRepository.enqueue(
                PlaybackEvent(
                    videoId = video.id,
                    eventType = "PROGRESS",
                    positionMs = positionMs,
                ),
            )
        }
    }

    fun onVideoStarted(video: FeedVideo) {
        viewModelScope.launch {
            playbackEventRepository.enqueue(
                PlaybackEvent(
                    videoId = video.id,
                    eventType = "PLAY",
                    positionMs = _uiState.value.resumePositionsMs[video.id] ?: 0L,
                ),
            )
        }
    }

    fun onToggleMute() {
        viewModelScope.launch {
            val nextMuted = !_uiState.value.isMuted
            feedPlaybackRepository.setMuted(nextMuted)
            _uiState.update { it.copy(isMuted = nextMuted) }
        }
    }

    fun getResumePositionMs(videoId: String): Long =
        _uiState.value.resumePositionsMs[videoId] ?: 0L

    override fun onCleared() {
        super.onCleared()
    }

    private fun loadInitialFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                feedRepository.loadFeedPage(limit = PAGE_SIZE)
            }.onSuccess { page ->
                nextCursor = page.nextCursor
                val resumePositions = page.videos.associate { video ->
                    video.id to feedPlaybackRepository.getResumePositionMs(video.id)
                }
                _uiState.update {
                    it.copy(
                        videos = page.videos,
                        isLoading = false,
                        hasMore = page.hasMore,
                        resumePositionsMs = resumePositions,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load feed",
                    )
                }
            }
        }
    }

    private fun loadMoreFeed() {
        viewModelScope.launch {
            isLoadingMore = true
            _uiState.update { it.copy(isLoadingMore = true) }
            runCatching {
                feedRepository.loadFeedPage(cursor = nextCursor, limit = PAGE_SIZE)
            }.onSuccess { page ->
                nextCursor = page.nextCursor
                val newResume = page.videos.associate { video ->
                    video.id to feedPlaybackRepository.getResumePositionMs(video.id)
                }
                _uiState.update { state ->
                    state.copy(
                        videos = state.videos + page.videos,
                        isLoadingMore = false,
                        hasMore = page.hasMore,
                        resumePositionsMs = state.resumePositionsMs + newResume,
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
            isLoadingMore = false
        }
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
