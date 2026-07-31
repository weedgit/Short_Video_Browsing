package com.shortvideo.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.shortvideo.composable.feed.VerticalVideoFeed
import com.shortvideo.core.DestinationRoute

@UnstableApi
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onSearchClick: () -> Unit = {},
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
                    selectedTab = uiState.selectedTab,
                    onTabSelected = viewModel::onTabSelected,
                    onSearchClick = onSearchClick,
                    commentsForActive = uiState.comments,
                    onLikeClick = viewModel::onLikeClick,
                    onFollowClick = viewModel::onFollowClick,
                    onSaveClick = viewModel::onSaveClick,
                    onShareClick = viewModel::onShareClick,
                    onLoadComments = viewModel::onLoadComments,
                    onSubmitComment = viewModel::onSubmitComment,
                    onFirstFrame = viewModel::onFirstFrame,
                )
            }
        }
    }
}

@UnstableApi
fun NavGraphBuilder.homeNavGraph(
    onSearchClick: () -> Unit = {},
) {
    composable(DestinationRoute.HOME_ROUTE) {
        HomeScreen(onSearchClick = onSearchClick)
    }
}
