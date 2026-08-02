package com.shortvideo.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.shortvideo.composable.feed.VerticalVideoFeed
import com.shortvideo.composable.feed.formatCount
import com.shortvideo.domain.model.FeedVideo
import com.shortvideo.domain.model.ProfileVideoItem
import com.shortvideo.domain.model.UserProfile
import com.shortvideo.theme.PrimaryColor
import com.shortvideo.theme.SubTextColor

@OptIn(UnstableApi::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val profile = uiState.profile
    if (profile == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(uiState.errorMessage ?: "Unable to load profile")
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = viewModel::refresh) { Text("Retry") }
            if (onNavigateBack != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onNavigateBack) { Text("Back") }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onNavigateToSettings) { Text("Account settings") }
            }
        }
        return
    }

    ProfileContent(
        profile = profile,
        isOtherProfile = uiState.isOtherProfile,
        selectedTab = uiState.selectedTab,
        gridItems = uiState.currentGridItems,
        isTabLoading = uiState.isTabLoading,
        onSettings = onNavigateToSettings,
        onNavigateBack = onNavigateBack,
        onFollowToggle = viewModel::toggleFollow,
        onSelectTab = viewModel::selectTab,
        onVideoClick = viewModel::openVideoViewer,
    )

    val viewerVideoId = uiState.viewerVideoId
    if (viewerVideoId != null) {
        val feedVideos = uiState.currentGridItems.mapNotNull { it.toFeedVideo(profile) }
        val startIndex = feedVideos.indexOfFirst { it.id == viewerVideoId }.coerceAtLeast(0)

        if (feedVideos.isNotEmpty()) {
            ProfileVideoViewerDialog(
                videos = feedVideos,
                startIndex = startIndex,
                isMuted = uiState.isMuted,
                onToggleMute = viewModel::onToggleMute,
                onClose = viewModel::closeVideoViewer,
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ProfileVideoViewerDialog(
    videos: List<FeedVideo>,
    startIndex: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            VerticalVideoFeed(
                videos = videos,
                isMuted = isMuted,
                resumePositionsMs = emptyMap(),
                onToggleMute = onToggleMute,
                onNearEnd = {},
                onActiveVideoChanged = { _, _ -> },
                onVideoStarted = {},
                initialPage = startIndex,
                showTopTabs = false,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 40.dp, start = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    isOtherProfile: Boolean,
    selectedTab: ProfileTab,
    gridItems: List<ProfileVideoItem>,
    isTabLoading: Boolean,
    onSettings: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    onFollowToggle: () -> Unit,
    onSelectTab: (ProfileTab) -> Unit,
    onVideoClick: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileHeader(
                profile = profile,
                isOtherProfile = isOtherProfile,
                onSettings = onSettings,
                onNavigateBack = onNavigateBack,
                onFollowToggle = onFollowToggle,
            )
        }

        if (!isOtherProfile) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ProfileTabRow(
                    selectedTab = selectedTab,
                    onSelectTab = onSelectTab,
                )
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 0.5.dp)
            }
        }

        if (isTabLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }
            }
        } else if (gridItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ProfileEmptyTab(
                    selectedTab = if (isOtherProfile) ProfileTab.VIDEOS else selectedTab,
                )
            }
        } else {
            items(gridItems, key = { "${selectedTab.name}-${it.id}" }) { video ->
                ProfileVideoCell(
                    video = video,
                    onClick = { onVideoClick(video.id) },
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    isOtherProfile: Boolean,
    onSettings: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    onFollowToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 12.dp),
    ) {
        if (isOtherProfile && onNavigateBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
                Text(
                    text = "@${profile.username}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Avatar left; display name + username (+ gear) → stats on the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = profile.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A))
                    .border(1.dp, Color(0xFF3A3A3A), CircleShape),
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = profile.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "@${profile.username}",
                            color = SubTextColor,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    if (profile.isSelf) {
                        IconButton(
                            onClick = onSettings,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ProfileStat(value = profile.followingCount, label = "Following")
                    Spacer(modifier = Modifier.width(20.dp))
                    ProfileStat(value = profile.followerCount, label = "Followers")
                    Spacer(modifier = Modifier.width(20.dp))
                    ProfileStat(value = profile.likeCount, label = "Likes")
                }
            }
        }

        if (!profile.bio.isNullOrBlank()) {
            Text(
                text = profile.bio!!,
                color = SubTextColor,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            )
        }

        if (isOtherProfile) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onFollowToggle,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (profile.isFollowing) Color(0xFF2A2A2A) else PrimaryColor,
                ),
            ) {
                Text(
                    text = if (profile.isFollowing) "Unfollow" else "Follow",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(
    value: Long,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatCount(value),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            color = SubTextColor,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ProfileTabRow(
    selectedTab: ProfileTab,
    onSelectTab: (ProfileTab) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(top = 4.dp),
        ) {
            ProfileTabItem(
                label = "Videos",
                icon = Icons.Default.GridView,
                selected = selectedTab == ProfileTab.VIDEOS,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(ProfileTab.VIDEOS) },
            )
            ProfileTabItem(
                label = "Favorites",
                icon = Icons.Default.Bookmark,
                selected = selectedTab == ProfileTab.FAVORITES,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(ProfileTab.FAVORITES) },
            )
            ProfileTabItem(
                label = "Liked",
                icon = Icons.Default.Favorite,
                selected = selectedTab == ProfileTab.LIKED,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(ProfileTab.LIKED) },
            )
        }
        HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 0.5.dp)
    }
}

@Composable
private fun ProfileTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = if (selected) Color.White else SubTextColor
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.45f)
                .background(if (selected) Color.White else Color.Transparent),
        )
    }
}

@Composable
private fun ProfileVideoCell(
    video: ProfileVideoItem,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(9f / 16f)
            .background(Color(0xFF111111))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = video.thumbnailUrl ?: video.id,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = formatCount(video.likeCount),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProfileEmptyTab(selectedTab: ProfileTab) {
    val (title, subtitle) = when (selectedTab) {
        ProfileTab.VIDEOS -> "No videos yet" to "Upload your first short video."
        ProfileTab.FAVORITES -> "No favorites yet" to "Save videos from the feed to find them here."
        ProfileTab.LIKED -> "No liked videos yet" to "Videos you like will appear in this private tab."
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = SubTextColor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
