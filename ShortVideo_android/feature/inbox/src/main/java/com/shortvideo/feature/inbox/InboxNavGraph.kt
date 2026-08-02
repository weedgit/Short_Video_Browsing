package com.shortvideo.feature.inbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.shortvideo.core.DestinationRoute
import com.shortvideo.domain.model.InboxNotification
import com.shortvideo.theme.PrimaryColor
import com.shortvideo.theme.SurfaceElevated

@Composable
fun InboxScreen(
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = uiState.selectedMessage

    if (selected != null) {
        BackHandler(onBack = viewModel::closeMessage)
        MessageDetailScreen(
            message = selected,
            onBack = viewModel::closeMessage,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Messages",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (uiState.unreadCount > 0) {
                    Text(
                        text = "${uiState.unreadCount} unread",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
            if (uiState.unreadCount > 0) {
                TextButton(onClick = viewModel::markAllRead) {
                    Text("Read all", color = PrimaryColor)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.notifications.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No messages yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = viewModel::refresh,
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            Text("Refresh")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(uiState.notifications, key = { it.id }) { item ->
                        MessageThreadRow(
                            item = item,
                            onClick = { viewModel.openMessage(item) },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 76.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageThreadRow(
    item: InboxNotification,
    onClick: () -> Unit,
) {
    val displayName = item.actorName?.takeIf { it.isNotBlank() }
        ?: item.title
    val preview = item.body

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessageAvatar(item = item)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.createdAtLabel,
                    color = if (item.isRead) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        PrimaryColor
                    },
                    fontSize = 12.sp,
                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = preview,
                    color = if (item.isRead) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    },
                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!item.isRead) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PrimaryColor),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageDetailScreen(
    message: InboxNotification,
    onBack: () -> Unit,
) {
    val headerName = message.actorName?.takeIf { it.isNotBlank() } ?: message.title

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            MessageAvatar(item = message, size = 36.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headerName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = messageTypeLabel(message.type),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = message.createdAtLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            item {
                IncomingMessageBubble(
                    title = message.title,
                    body = message.body,
                )
            }
        }
    }
}

@Composable
private fun IncomingMessageBubble(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(SurfaceElevated)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun MessageAvatar(
    item: InboxNotification,
    size: androidx.compose.ui.unit.Dp = 52.dp,
) {
    val avatarUrl = item.actorAvatarUrl
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = item.actorName ?: item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(SurfaceElevated),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(avatarColor(item.type)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = avatarIcon(item.type),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.45f),
            )
        }
    }
}

private fun avatarIcon(type: String): ImageVector =
    when (type.uppercase()) {
        "ANNOUNCEMENT" -> Icons.Default.Campaign
        "LIKE" -> Icons.Default.Favorite
        "FOLLOW" -> Icons.Default.PersonAdd
        "COMMENT" -> Icons.Default.ChatBubble
        else -> Icons.Default.Notifications
    }

private fun avatarColor(type: String): Color =
    when (type.uppercase()) {
        "ANNOUNCEMENT" -> Color(0xFF3D5AFE)
        "LIKE" -> PrimaryColor
        "FOLLOW" -> Color(0xFF2BB673)
        "COMMENT" -> Color(0xFF00BCD4)
        else -> Color(0xFF455A64)
    }

private fun messageTypeLabel(type: String): String =
    when (type.uppercase()) {
        "ANNOUNCEMENT" -> "Announcement"
        "LIKE" -> "Like"
        "FOLLOW" -> "Follower"
        "COMMENT" -> "Comment"
        else -> type.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
    }

fun NavGraphBuilder.inboxNavGraph() {
    composable(DestinationRoute.INBOX_ROUTE) {
        InboxScreen()
    }
}
