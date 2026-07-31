package com.shortvideo.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.composable.feed.FeedTab
import com.shortvideo.domain.model.FeedVideo
import com.shortvideo.domain.model.PlaybackEvent
import com.shortvideo.domain.model.VideoComment
import com.shortvideo.domain.repository.FeedPlaybackRepository
import com.shortvideo.domain.repository.FeedRepository
import com.shortvideo.domain.repository.PlaybackEventRepository
import com.shortvideo.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val videos: List<FeedVideo> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val isMuted: Boolean = false,
    val resumePositionsMs: Map<String, Long> = emptyMap(),
    val errorMessage: String? = null,
    val selectedTab: FeedTab = FeedTab.ForYou,
    val comments: List<VideoComment> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val playbackEventRepository: PlaybackEventRepository,
    private val feedPlaybackRepository: FeedPlaybackRepository,
    private val socialRepository: SocialRepository,
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

    fun onTabSelected(tab: FeedTab) {
        if (tab == _uiState.value.selectedTab) return
        nextCursor = null
        _uiState.update {
            it.copy(
                selectedTab = tab,
                videos = emptyList(),
                isLoading = true,
                errorMessage = null,
            )
        }
        loadInitialFeed()
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
            _uiState.update { state ->
                state.copy(
                    resumePositionsMs = state.resumePositionsMs + (video.id to positionMs),
                )
            }
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

    fun onFirstFrame(video: FeedVideo, ttffMs: Long) {
        viewModelScope.launch {
            playbackEventRepository.enqueue(
                PlaybackEvent(
                    videoId = video.id,
                    eventType = "TTFF",
                    positionMs = ttffMs.coerceAtLeast(0L),
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

    fun onLikeClick(video: FeedVideo) {
        optimisticUpdate(video.id) { current ->
            val liked = !current.isLiked
            current.copy(
                isLiked = liked,
                likeCount = (current.likeCount + if (liked) 1 else -1).coerceAtLeast(0),
            )
        }
        viewModelScope.launch {
            runCatching {
                if (!video.isLiked) {
                    socialRepository.likeVideo(video.id)
                } else {
                    socialRepository.unlikeVideo(video.id)
                }
            }.onSuccess { (liked, count) ->
                updateVideo(video.id) { it.copy(isLiked = liked, likeCount = count) }
            }.onFailure {
                optimisticUpdate(video.id) { video }
            }
        }
    }

    fun onFollowClick(video: FeedVideo) {
        val authorId = video.authorId ?: return
        optimisticUpdate(video.id) { it.copy(isFollowing = !it.isFollowing) }
        viewModelScope.launch {
            runCatching {
                if (!video.isFollowing) {
                    socialRepository.followUser(authorId)
                } else {
                    socialRepository.unfollowUser(authorId)
                }
            }.onSuccess { following ->
                _uiState.update { state ->
                    state.copy(
                        videos = state.videos.map { item ->
                            if (item.authorId == authorId) item.copy(isFollowing = following) else item
                        },
                    )
                }
            }.onFailure {
                optimisticUpdate(video.id) { video }
            }
        }
    }

    fun onSaveClick(video: FeedVideo) {
        optimisticUpdate(video.id) { it.copy(isSaved = !it.isSaved) }
        viewModelScope.launch {
            runCatching {
                if (!video.isSaved) socialRepository.saveVideo(video.id)
                else socialRepository.unsaveVideo(video.id)
            }.onSuccess { saved ->
                updateVideo(video.id) { it.copy(isSaved = saved) }
            }.onFailure {
                optimisticUpdate(video.id) { video }
            }
        }
    }

    fun onShareClick(video: FeedVideo) {
        updateVideo(video.id) { it.copy(shareCount = it.shareCount + 1) }
    }

    fun onLoadComments(video: FeedVideo) {
        viewModelScope.launch {
            runCatching { socialRepository.getComments(video.id) }
                .onSuccess { comments ->
                    _uiState.update { it.copy(comments = comments) }
                }
                .onFailure {
                    _uiState.update { it.copy(comments = emptyList()) }
                }
        }
    }

    fun onSubmitComment(video: FeedVideo, text: String) {
        viewModelScope.launch {
            runCatching { socialRepository.postComment(video.id, text) }
                .onSuccess { comment ->
                    _uiState.update { state ->
                        state.copy(
                            comments = state.comments + comment,
                            videos = state.videos.map { item ->
                                if (item.id == video.id) {
                                    item.copy(commentCount = item.commentCount + 1)
                                } else {
                                    item
                                }
                            },
                        )
                    }
                }
                .onFailure {
                    val local = VideoComment(
                        id = "local-${System.currentTimeMillis()}",
                        videoId = video.id,
                        authorName = "You",
                        text = text,
                        createdAtLabel = "Just now",
                    )
                    _uiState.update { state ->
                        state.copy(
                            comments = state.comments + local,
                            videos = state.videos.map { item ->
                                if (item.id == video.id) {
                                    item.copy(commentCount = item.commentCount + 1)
                                } else {
                                    item
                                }
                            },
                        )
                    }
                }
        }
    }

    private fun optimisticUpdate(videoId: String, transform: (FeedVideo) -> FeedVideo) {
        updateVideo(videoId, transform)
    }

    private fun updateVideo(videoId: String, transform: (FeedVideo) -> FeedVideo) {
        _uiState.update { state ->
            state.copy(
                videos = state.videos.map { if (it.id == videoId) transform(it) else it },
            )
        }
    }

    private fun loadInitialFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val tab = tabQuery(_uiState.value.selectedTab)
            runCatching {
                feedRepository.loadFeedPage(limit = PAGE_SIZE, tab = tab)
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
                        errorMessage = if (page.videos.isEmpty()) {
                            if (tab == "following") "Follow creators to see their videos here."
                            else null
                        } else {
                            null
                        },
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
            val tab = tabQuery(_uiState.value.selectedTab)
            runCatching {
                feedRepository.loadFeedPage(cursor = nextCursor, limit = PAGE_SIZE, tab = tab)
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

    private fun tabQuery(tab: FeedTab): String =
        when (tab) {
            FeedTab.ForYou -> "foryou"
            FeedTab.Following -> "following"
        }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
