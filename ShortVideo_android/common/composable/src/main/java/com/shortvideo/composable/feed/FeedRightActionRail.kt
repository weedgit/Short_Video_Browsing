package com.shortvideo.composable.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shortvideo.domain.model.FeedVideo
import com.shortvideo.theme.PrimaryColor

@Composable
fun FeedRightActionRail(
    video: FeedVideo,
    onAvatarClick: () -> Unit = {},
    onFollowClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(end = 12.dp, bottom = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            AsyncImage(
                model = video.authorAvatarUrl,
                contentDescription = video.authorName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color.DarkGray)
                    .clickable(onClick = onAvatarClick),
            )
            if (!video.isFollowing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(PrimaryColor)
                        .clickable(onClick = onFollowClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Follow",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        RailAction(
            icon = if (video.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            tint = if (video.isLiked) PrimaryColor else Color.White,
            label = formatCount(video.likeCount),
            onClick = onLikeClick,
        )
        RailAction(
            icon = Icons.Default.ChatBubble,
            tint = Color.White,
            label = formatCount(video.commentCount),
            onClick = onCommentClick,
        )
        RailAction(
            icon = if (video.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            tint = if (video.isSaved) Color(0xFFFFD700) else Color.White,
            label = "Save",
            onClick = onSaveClick,
        )
        RailAction(
            icon = Icons.Default.Share,
            tint = Color.White,
            label = formatCount(video.shareCount),
            onClick = onShareClick,
        )
    }
}

@Composable
private fun RailAction(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
    else -> count.toString()
}
