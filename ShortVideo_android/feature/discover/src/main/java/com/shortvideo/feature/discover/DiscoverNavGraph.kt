package com.shortvideo.feature.discover

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil.compose.AsyncImage
import com.shortvideo.composable.feed.formatCount
import com.shortvideo.core.DestinationRoute
import com.shortvideo.domain.model.DiscoverTab
import com.shortvideo.domain.model.DiscoverVideo
import com.shortvideo.domain.model.UserProfile
import com.shortvideo.theme.PrimaryColor
import com.shortvideo.theme.SubTextColor
import com.shortvideo.theme.SurfaceElevated

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    ) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(searchPlaceholder(uiState.selectedTab)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )

        DiscoverTabRow(
            selectedTab = uiState.selectedTab,
            onSelectTab = viewModel::onTabSelected,
            modifier = Modifier.padding(top = 12.dp),
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = uiState.errorMessage.orEmpty(), color = SubTextColor)
                }
            }
            uiState.selectedTab == DiscoverTab.VIDEOS -> {
                DiscoverVideosContent(
                    hashtags = uiState.hashtags,
                    videos = uiState.videos,
                    onHashtagClick = viewModel::onQueryChange,
                )
            }
            else -> {
                DiscoverUsersContent(
                    users = uiState.users,
                    emptyMessage = when (uiState.selectedTab) {
                        DiscoverTab.FRIENDS -> "Follow creators to see them here"
                        else -> "No users found"
                    },
                    onFollowToggle = viewModel::onFollowToggle,
                )
            }
        }
    }
}

@Composable
private fun DiscoverTabRow(
    selectedTab: DiscoverTab,
    onSelectTab: (DiscoverTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            DiscoverTabItem(
                label = "Videos",
                icon = Icons.Default.Videocam,
                selected = selectedTab == DiscoverTab.VIDEOS,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(DiscoverTab.VIDEOS) },
            )
            DiscoverTabItem(
                label = "Users",
                icon = Icons.Default.PersonSearch,
                selected = selectedTab == DiscoverTab.USERS,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(DiscoverTab.USERS) },
            )
            DiscoverTabItem(
                label = "Friends",
                icon = Icons.Default.People,
                selected = selectedTab == DiscoverTab.FRIENDS,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(DiscoverTab.FRIENDS) },
            )
        }
        HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 0.5.dp)
    }
}

@Composable
private fun DiscoverTabItem(
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
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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
private fun DiscoverVideosContent(
    hashtags: List<com.shortvideo.domain.model.DiscoverHashtag>,
    videos: List<DiscoverVideo>,
    onHashtagClick: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (hashtags.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "Trending hashtags",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(hashtags) { tag ->
                            Text(
                                text = "${tag.tag} · ${formatCount(tag.videoCount)}",
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(PrimaryColor.copy(alpha = 0.85f))
                                    .clickable { onHashtagClick(tag.tag) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Videos",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
            }
        }

        if (videos.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "No videos found", color = SubTextColor)
                }
            }
        } else {
            items(videos, key = { it.id }) { video ->
                DiscoverVideoCell(video = video)
            }
        }
    }
}

@Composable
private fun DiscoverVideoCell(video: DiscoverVideo) {
    Box(
        modifier = Modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevated),
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.description,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = "@${video.authorName}",
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        )
    }
}

@Composable
private fun DiscoverUsersContent(
    users: List<UserProfile>,
    emptyMessage: String,
    onFollowToggle: (UserProfile) -> Unit,
) {
    if (users.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emptyMessage, color = SubTextColor)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(users, key = { it.id }) { user ->
            DiscoverUserRow(
                user = user,
                onFollowToggle = { onFollowToggle(user) },
            )
        }
    }
}

@Composable
private fun DiscoverUserRow(
    user: UserProfile,
    onFollowToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = user.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceElevated),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${user.username}",
                color = SubTextColor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onFollowToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (user.isFollowing) Color(0xFF2A2A2A) else PrimaryColor,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            modifier = Modifier
                .height(32.dp)
                .then(
                    if (user.isFollowing) {
                        Modifier.border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(8.dp))
                    } else {
                        Modifier
                    },
                ),
        ) {
            Text(
                text = if (user.isFollowing) "Following" else "Follow",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun searchPlaceholder(tab: DiscoverTab): String =
    when (tab) {
        DiscoverTab.VIDEOS -> "Search hashtags, videos"
        DiscoverTab.USERS -> "Search users"
        DiscoverTab.FRIENDS -> "Search friends"
    }

fun NavGraphBuilder.discoverNavGraph() {
    composable(DestinationRoute.DISCOVER_ROUTE) {
        DiscoverScreen()
    }
}
