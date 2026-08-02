package com.shortvideo.feature.discover

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.shortvideo.theme.SurfaceElevated

@Composable
fun DiscoverScreen(
    onUserClick: (String) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = PrimaryColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurface,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
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
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                    onUserClick = onUserClick,
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
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
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
    val tint = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
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
                .background(
                    if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                ),
        )
    }
}

@Composable
private fun DiscoverVideosContent(
    hashtags: List<com.shortvideo.domain.model.DiscoverHashtag>,
    videos: List<DiscoverVideo>,
    onHashtagClick: (String) -> Unit,
) {
    // Keep hashtags pinned above the grid so vertical scroll doesn't fight LazyRow.
    Column(modifier = Modifier.fillMaxSize()) {
        if (hashtags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(hashtags, key = { it.tag }) { tag ->
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
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                top = if (hashtags.isEmpty()) 12.dp else 0.dp,
                bottom = 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (videos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No videos found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(videos, key = { it.id }) { video ->
                    DiscoverVideoCell(video = video)
                }
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
    onUserClick: (String) -> Unit,
    onFollowToggle: (UserProfile) -> Unit,
) {
    if (users.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                onUserClick = { onUserClick(user.id) },
                onFollowToggle = { onFollowToggle(user) },
            )
        }
    }
}

@Composable
private fun DiscoverUserRow(
    user: UserProfile,
    onUserClick: () -> Unit,
    onFollowToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${user.username}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onFollowToggle,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (user.isFollowing) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    PrimaryColor
                },
                contentColor = if (user.isFollowing) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color.White
                },
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
        ) {
            Text(
                text = if (user.isFollowing) "Unfollow" else "Follow",
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

fun NavGraphBuilder.discoverNavGraph(
    onUserClick: (String) -> Unit,
) {
    composable(DestinationRoute.DISCOVER_ROUTE) {
        DiscoverScreen(onUserClick = onUserClick)
    }
}
