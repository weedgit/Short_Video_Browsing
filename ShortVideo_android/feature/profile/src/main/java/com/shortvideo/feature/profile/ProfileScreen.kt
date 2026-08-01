package com.shortvideo.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.shortvideo.composable.feed.formatCount
import com.shortvideo.domain.model.ProfileVideoItem
import com.shortvideo.domain.model.UserProfile
import com.shortvideo.theme.PrimaryColor

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.onAvatarSelected(uri)
    }

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
        videos = uiState.videos,
        isUploadingAvatar = uiState.isUploadingAvatar,
        editError = uiState.editError,
        onSettings = onNavigateToSettings,
        onFollowToggle = viewModel::toggleFollow,
        onEditProfile = viewModel::openEditProfile,
        onChangeAvatar = {
            avatarPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onClearEditError = viewModel::clearEditError,
    )

    if (uiState.showEditSheet) {
        EditProfileDialog(
            profile = profile,
            isSaving = uiState.isSaving,
            errorMessage = uiState.editError,
            onDismiss = viewModel::dismissEditProfile,
            onSave = viewModel::saveProfile,
        )
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    videos: List<ProfileVideoItem>,
    isUploadingAvatar: Boolean,
    editError: String?,
    onSettings: () -> Unit,
    onFollowToggle: () -> Unit,
    onEditProfile: () -> Unit,
    onChangeAvatar: () -> Unit,
    onClearEditError: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = profile.username,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = profile.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                        .then(
                            if (profile.isSelf) {
                                Modifier.clickable(onClick = onChangeAvatar)
                            } else {
                                Modifier
                            },
                        ),
                )
                if (isUploadingAvatar) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color.White,
                        strokeWidth = 3.dp,
                    )
                } else if (profile.isSelf) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrimaryColor)
                            .clickable(onClick = onChangeAvatar),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change avatar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(profile.displayName, fontWeight = FontWeight.Bold)
            if (!profile.bio.isNullOrBlank()) {
                Text(
                    text = profile.bio!!,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Stat(label = "Following", value = profile.followingCount)
                Stat(label = "Followers", value = profile.followerCount)
                Stat(label = "Likes", value = videos.sumOf { it.likeCount })
            }
            if (profile.isSelf) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onEditProfile) {
                    Text("Edit profile")
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onFollowToggle,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (profile.isFollowing) Color.DarkGray else PrimaryColor,
                    ),
                ) {
                    Text(if (profile.isFollowing) "Following" else "Follow")
                }
            }
            if (!editError.isNullOrBlank() && !isUploadingAvatar) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = editError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable(onClick = onClearEditError),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(videos, key = { it.id }) { video ->
                Box(
                    modifier = Modifier
                        .aspectRatio(9f / 16f)
                        .background(Color.Black)
                        .clickable { },
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        text = formatCount(video.likeCount),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (displayName: String, bio: String) -> Unit,
) {
    var displayName by remember(profile.id) { mutableStateOf(profile.displayName) }
    var bio by remember(profile.id) { mutableStateOf(profile.bio.orEmpty()) }

    LaunchedEffect(profile.displayName, profile.bio) {
        if (!isSaving) {
            displayName = profile.displayName
            bio = profile.bio.orEmpty()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { if (it.length <= 50) displayName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 200) bio = it },
                    label = { Text("Bio") },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(displayName, bio) },
                enabled = !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun Stat(label: String, value: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(formatCount(value), fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
