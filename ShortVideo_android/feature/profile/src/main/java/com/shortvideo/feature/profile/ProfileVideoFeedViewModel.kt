package com.shortvideo.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.core.DestinationRoute
import com.shortvideo.domain.model.FeedVideo
import com.shortvideo.domain.model.PlaybackEvent
import com.shortvideo.domain.model.VideoComment
import com.shortvideo.domain.repository.FeedPlaybackRepository
import com.shortvideo.domain.repository.PlaybackEventRepository
import com.shortvideo.domain.repository.ProfileRepository
import com.shortvideo.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileVideoSource(val apiValue: String) {
    VIDEOS("videos"),
    LIKED("liked"),
    SAVED("saved"),
    ;

    companion object {
        fun from(value: String?): ProfileVideoSource =
            entries.firstOrNull { it.apiValue == value } ?: VIDEOS
    }
}

data class ProfileVideoFeedUiState(
    val videos: List<FeedVideo> = emptyList(),
    val startVideoId: String? = null,
    val isLoading: Boolean = true,
    val isMuted: Boolean = false,
    val resumePositionsMs: Map<String, Long> = emptyMap(),
    val comments: List<VideoComment> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class ProfileVideoFeedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val socialRepository: SocialRepository,
    private val feedPlaybackRepository: FeedPlaybackRepository,
    private val playbackEventRepository: PlaybackEventRepository,
) : ViewModel() {
    private val source = ProfileVideoSource.from(
        savedStateHandle.get<String>(DestinationRoute.PROFILE_VIDEO_SOURCE_ARG),
    )
    private val ownerId =
        savedStateHandle.get<String>(DestinationRoute.PROFILE_VIDEO_OWNER_ARG).orEmpty()
    private val startVideoId =
        savedStateHandle.get<String>(DestinationRoute.PROFILE_VIDEO_ID_ARG)

    private val _uiState = MutableStateFlow(
        ProfileVideoFeedUiState(startVideoId = startVideoId),
    )
    val uiState: StateFlow<ProfileVideoFeedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val muted = feedPlaybackRepository.isMuted()
            _uiState.update { it.copy(isMuted = muted) }
            loadVideos()
        }
    }

    private fun loadVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                when (source) {
                    ProfileVideoSource.VIDEOS ->
                        profileRepository.getProfileFeedVideos(ownerId)
                    ProfileVideoSource.LIKED ->
                        profileRepository.getMyLikedFeedVideos()
                    ProfileVideoSource.SAVED ->
                        profileRepository.getMySavedFeedVideos()
                }
            }.onSuccess { videos ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        videos = videos,
                        errorMessage = if (videos.isEmpty()) "No videos available." else null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load videos",
                    )
                }
            }
        }
    }

    fun onNearEnd(@Suppress("UNUSED_PARAMETER") currentIndex: Int) = Unit

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
            onLoadComments(video)
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
                if (!video.isLiked) socialRepository.likeVideo(video.id)
                else socialRepository.unlikeVideo(video.id)
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
                if (!video.isFollowing) socialRepository.followUser(authorId)
                else socialRepository.unfollowUser(authorId)
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

    fun onShareClick(@Suppress("UNUSED_PARAMETER") video: FeedVideo) = Unit

    fun onReportVideo(video: FeedVideo, title: String, content: String) {
        viewModelScope.launch {
            runCatching {
                socialRepository.reportVideo(video.id, title, content)
            }
        }
    }

    fun onLoadComments(video: FeedVideo) {
        viewModelScope.launch {
            runCatching { socialRepository.getComments(video.id) }
                .onSuccess { comments ->
                    _uiState.update { it.copy(comments = comments) }
                }
        }
    }

    fun onSubmitComment(video: FeedVideo, text: String, parentId: String?) {
        viewModelScope.launch {
            runCatching { socialRepository.postComment(video.id, text, parentId) }
                .onSuccess {
                    onLoadComments(video)
                    updateVideo(video.id) { it.copy(commentCount = it.commentCount + 1) }
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
}
