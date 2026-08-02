package com.shortvideo.data.mapper

import com.shortvideo.data.remote.dto.CommentDto
import com.shortvideo.data.remote.dto.DiscoverHashtagDto
import com.shortvideo.data.remote.dto.DiscoverVideoDto
import com.shortvideo.data.remote.dto.FeedPageDto
import com.shortvideo.data.remote.dto.FeedVideoDto
import com.shortvideo.data.remote.dto.InboxNotificationDto
import com.shortvideo.data.remote.dto.ProfileVideoItemDto
import com.shortvideo.data.remote.dto.UserProfileDto
import com.shortvideo.domain.model.DiscoverHashtag
import com.shortvideo.domain.model.DiscoverVideo
import com.shortvideo.domain.model.FeedPage
import com.shortvideo.domain.model.FeedVideo
import com.shortvideo.domain.model.InboxNotification
import com.shortvideo.domain.model.ProfileVideoItem
import com.shortvideo.domain.model.UserProfile
import com.shortvideo.domain.model.VideoComment
import java.time.Duration
import java.time.Instant

fun FeedPageDto.toDomain(): FeedPage =
    FeedPage(
        videos = items.map { it.toDomain() },
        nextCursor = nextCursor,
        hasMore = hasMore,
    )

fun FeedVideoDto.toDomain(): FeedVideo =
    FeedVideo(
        id = id,
        streamUrl = streamUrl,
        authorName = authorName,
        description = description,
        hashtags = hashtags,
        category = category,
        uploadedAtLabel = uploadedAtLabel,
        durationMs = durationMs,
        playbackFormat = playbackFormat,
        streamUrlExpiresAt = streamUrlExpiresAt,
        thumbnailUrl = thumbnailUrl,
        authorId = authorId,
        authorAvatarUrl = authorAvatarUrl,
        likeCount = likeCount,
        commentCount = commentCount,
        shareCount = shareCount,
        isLiked = isLiked,
        isFollowing = isFollowing,
        isSaved = isSaved,
        musicLabel = musicLabel,
    )

fun CommentDto.toDomain(): VideoComment =
    VideoComment(
        id = id,
        videoId = videoId,
        authorName = authorName,
        authorAvatarUrl = authorAvatarUrl,
        text = text,
        createdAtLabel = createdAtLabel?.takeIf { it.isNotBlank() }
            ?: formatRelativeTime(createdAt),
        parentId = parentId,
        replyToAuthorName = replyToAuthorName,
        replyCount = replyCount.coerceAtLeast(replies.size),
        replies = replies.map { it.toDomain() },
    )

private fun formatRelativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        val instant = Instant.parse(iso)
        val seconds = Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0)
        when {
            seconds < 60 -> "Just now"
            seconds < 3600 -> "${seconds / 60}m"
            seconds < 86400 -> "${seconds / 3600}h"
            seconds < 604800 -> "${seconds / 86400}d"
            else -> "${seconds / 604800}w"
        }
    }.getOrDefault("")
}

fun UserProfileDto.toDomain(): UserProfile =
    UserProfile(
        id = id,
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        bio = bio,
        followerCount = followerCount,
        followingCount = followingCount,
        videoCount = videoCount,
        likeCount = likeCount,
        isFollowing = isFollowing,
        isSelf = isSelf || isMe,
    )

fun ProfileVideoItemDto.toDomain(): ProfileVideoItem =
    ProfileVideoItem(
        id = id,
        thumbnailUrl = thumbnailUrl,
        likeCount = likeCount,
        durationMs = durationMs,
    )

fun DiscoverHashtagDto.toDomain(): DiscoverHashtag =
    DiscoverHashtag(tag = tag, videoCount = videoCount)

fun DiscoverVideoDto.toDomain(): DiscoverVideo =
    DiscoverVideo(
        id = id,
        description = description,
        thumbnailUrl = thumbnailUrl,
        likeCount = likeCount,
        authorId = authorId,
        authorName = authorName,
        authorAvatarUrl = authorAvatarUrl,
    )

fun InboxNotificationDto.toDomain(): InboxNotification =
    InboxNotification(
        id = id,
        type = type,
        title = title,
        body = body,
        isRead = isRead,
        createdAtLabel = createdAtLabel?.takeIf { it.isNotBlank() }
            ?: formatRelativeTime(createdAt),
        videoId = videoId,
        actorUserId = actorUserId,
    )

fun List<FeedVideo>.toMockPage(): FeedPage =
    FeedPage(
        videos = this,
        nextCursor = null,
        hasMore = false,
    )
