package com.shortvideo.feature.profile

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.shortvideo.composable.feed.formatCount
import com.shortvideo.domain.model.ProfileVideoItem
import com.shortvideo.domain.model.UserProfile
import com.shortvideo.theme.PrimaryColor
import com.shortvideo.theme.SubTextColor

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
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
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateToSettings) { Text("Account settings") }
        }
        return
    }

    ProfileContent(
        profile = profile,
        selectedTab = uiState.selectedTab,
        gridItems = uiState.currentGridItems,
        isTabLoading = uiState.isTabLoading,
        onSettings = onNavigateToSettings,
        onFollowToggle = viewModel::toggleFollow,
        onSelectTab = viewModel::selectTab,
    )
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    selectedTab: ProfileTab,
    gridItems: List<ProfileVideoItem>,
    isTabLoading: Boolean,
    onSettings: () -> Unit,
    onFollowToggle: () -> Unit,
    onSelectTab: (ProfileTab) -> Unit,
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
                onSettings = onSettings,
                onFollowToggle = onFollowToggle,
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileTabRow(
                selectedTab = selectedTab,
                onSelectTab = onSelectTab,
            )
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
                ProfileEmptyTab(selectedTab = selectedTab)
            }
        } else {
            items(gridItems, key = { "${selectedTab.name}-${it.id}" }) { video ->
                ProfileVideoCell(video = video)
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    onSettings: () -> Unit,
    onFollowToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "@${profile.username}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row {
                if (profile.isSelf) {
                    IconButton(onClick = onSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                        )
                    }
                }
                IconButton(onClick = onSettings) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // TikTok-style: avatar on the left, personal info on the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = profile.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A))
                    .border(1.dp, Color(0xFF3A3A3A), CircleShape),
            )

            Spacer(modifier = Modifier.width(20.dp))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileStat(value = profile.followingCount, label = "Following")
                ProfileStat(value = profile.followerCount, label = "Followers")
                ProfileStat(value = profile.likeCount, label = "Likes")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = profile.displayName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )

        if (!profile.bio.isNullOrBlank()) {
            Text(
                text = profile.bio!!,
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (profile.isSelf) {
            OutlinedButton(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A3A)),
            ) {
                Text("Edit profile", fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onFollowToggle,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (profile.isFollowing) Color(0xFF2A2A2A) else PrimaryColor,
                    ),
                ) {
                    Text(
                        text = if (profile.isFollowing) "Following" else "Follow",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A3A)),
                ) {
                    Text("Message", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.width(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A3A)),
                ) {
                    Icon(Icons.Default.PersonAddAlt, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatCount(value),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
        )
        Text(
            text = label,
            color = SubTextColor,
            style = MaterialTheme.typography.labelMedium,
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
                showLock = false,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(ProfileTab.VIDEOS) },
            )
            ProfileTabItem(
                label = "Favorites",
                icon = Icons.Default.Bookmark,
                selected = selectedTab == ProfileTab.FAVORITES,
                showLock = true,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(ProfileTab.FAVORITES) },
            )
            ProfileTabItem(
                label = "Liked",
                icon = Icons.Default.Favorite,
                selected = selectedTab == ProfileTab.LIKED,
                showLock = true,
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
    showLock: Boolean,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
            if (showLock) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Private",
                    tint = tint,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
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
private fun ProfileVideoCell(video: ProfileVideoItem) {
    Box(
        modifier = Modifier
            .aspectRatio(9f / 16f)
            .background(Color(0xFF111111))
            .clickable { },
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
