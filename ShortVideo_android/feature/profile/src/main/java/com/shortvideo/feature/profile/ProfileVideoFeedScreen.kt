package com.shortvideo.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.shortvideo.composable.feed.VerticalVideoFeed

@UnstableApi
@Composable
fun ProfileVideoFeedScreen(
    onNavigateBack: () -> Unit,
    onAvatarClick: (authorId: String) -> Unit,
    viewModel: ProfileVideoFeedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            uiState.videos.isEmpty() -> {
                Text(
                    text = uiState.errorMessage ?: "No videos available.",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            else -> {
                VerticalVideoFeed(
                    videos = uiState.videos,
                    isMuted = uiState.isMuted,
                    resumePositionsMs = uiState.resumePositionsMs,
                    onToggleMute = viewModel::onToggleMute,
                    onNearEnd = viewModel::onNearEnd,
                    onActiveVideoChanged = viewModel::onActiveVideoChanged,
                    onVideoStarted = viewModel::onVideoStarted,
                    commentsForActive = uiState.comments,
                    onLikeClick = viewModel::onLikeClick,
                    onFollowClick = viewModel::onFollowClick,
                    onSaveClick = viewModel::onSaveClick,
                    onShareClick = viewModel::onShareClick,
                    onLoadComments = viewModel::onLoadComments,
                    onSubmitComment = viewModel::onSubmitComment,
                    onReportVideo = viewModel::onReportVideo,
                    onFirstFrame = viewModel::onFirstFrame,
                    showTopTabs = false,
                    initialVideoId = uiState.startVideoId,
                    onAvatarClick = { video ->
                        val authorId = video.authorId
                        if (!authorId.isNullOrBlank()) {
                            onAvatarClick(authorId)
                        }
                    },
                )
            }
        }

        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(4.dp)
                .align(Alignment.TopStart),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }
    }
}
