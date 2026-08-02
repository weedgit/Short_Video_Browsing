package com.shortvideo.domain.model

data class FeedPage(
    val videos: List<FeedVideo>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

data class FeedVideo(
    val id: String,
    val streamUrl: String,
    val authorName: String,
    val description: String,
    val hashtags: List<String>,
    val category: String? = null,
    val uploadedAtLabel: String,
    val durationMs: Long,
    val playbackFormat: String = "mp4",
    val streamUrlExpiresAt: String? = null,
    val thumbnailUrl: String? = null,
    val authorId: String? = null,
    val authorAvatarUrl: String? = null,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val shareCount: Long = 0,
    val isLiked: Boolean = false,
    val isFollowing: Boolean = false,
    val isSaved: Boolean = false,
    val musicLabel: String? = null,
)

data class PlaybackEvent(
    val videoId: String,
    val eventType: String,
    val positionMs: Long,
)

data class VideoComment(
    val id: String,
    val videoId: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val text: String,
    val createdAtLabel: String,
)

data class UserProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    val videoCount: Long = 0,
    val likeCount: Long = 0,
    val isFollowing: Boolean = false,
    val isSelf: Boolean = false,
)

data class ProfileVideoItem(
    val id: String,
    val thumbnailUrl: String?,
    val likeCount: Long,
    val durationMs: Long,
)

data class DiscoverHashtag(
    val tag: String,
    val videoCount: Long,
)

data class InboxNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val createdAtLabel: String,
    val videoId: String? = null,
    val actorUserId: String? = null,
)
