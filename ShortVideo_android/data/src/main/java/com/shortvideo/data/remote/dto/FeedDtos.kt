package com.shortvideo.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FeedPageDto(
    @SerializedName("items") val items: List<FeedVideoDto>,
    @SerializedName("nextCursor") val nextCursor: String?,
    @SerializedName("hasMore") val hasMore: Boolean,
)

data class FeedVideoDto(
    @SerializedName("id") val id: String,
    @SerializedName("streamUrl") val streamUrl: String,
    @SerializedName("playbackFormat") val playbackFormat: String,
    @SerializedName("streamUrlExpiresAt") val streamUrlExpiresAt: String?,
    @SerializedName("authorName") val authorName: String,
    @SerializedName("description") val description: String,
    @SerializedName("hashtags") val hashtags: List<String>,
    @SerializedName("category") val category: String?,
    @SerializedName("uploadedAtLabel") val uploadedAtLabel: String,
    @SerializedName("durationMs") val durationMs: Long,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerializedName("authorId") val authorId: String? = null,
    @SerializedName("authorAvatarUrl") val authorAvatarUrl: String? = null,
    @SerializedName("likeCount") val likeCount: Long = 0,
    @SerializedName("commentCount") val commentCount: Long = 0,
    @SerializedName("shareCount") val shareCount: Long = 0,
    @SerializedName("isLiked") val isLiked: Boolean = false,
    @SerializedName("isFollowing") val isFollowing: Boolean = false,
    @SerializedName("isSaved") val isSaved: Boolean = false,
    @SerializedName("musicLabel") val musicLabel: String? = null,
)

data class PlaybackEventRequestDto(
    @SerializedName("videoId") val videoId: String,
    @SerializedName("eventType") val eventType: String,
    @SerializedName("positionMs") val positionMs: Long,
    @SerializedName("occurredAt") val occurredAt: String? = null,
)

data class PlaybackBatchRequestDto(
    @SerializedName("events") val events: List<PlaybackEventRequestDto>,
)

data class PlaybackBatchResponseDto(
    @SerializedName("accepted") val accepted: Int,
)

data class CommentDto(
    @SerializedName("id") val id: String,
    @SerializedName("videoId") val videoId: String,
    @SerializedName("authorName") val authorName: String,
    @SerializedName("authorAvatarUrl") val authorAvatarUrl: String? = null,
    @SerializedName("text") val text: String,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("createdAtLabel") val createdAtLabel: String? = null,
    @SerializedName("parentId") val parentId: String? = null,
    @SerializedName("replyToAuthorName") val replyToAuthorName: String? = null,
    @SerializedName("replyCount") val replyCount: Int = 0,
    @SerializedName("replies") val replies: List<CommentDto> = emptyList(),
)

data class CommentListDto(
    @SerializedName("items") val items: List<CommentDto>,
)

data class CreateCommentRequestDto(
    @SerializedName("text") val text: String,
    @SerializedName("parentId") val parentId: String? = null,
)

data class CreateReportRequestDto(
    @SerializedName("targetType") val targetType: String,
    @SerializedName("targetId") val targetId: String,
    @SerializedName("reason") val reason: String,
)

data class LikeResponseDto(
    @SerializedName("liked") val liked: Boolean,
    @SerializedName("likeCount") val likeCount: Long,
)

data class FollowResponseDto(
    @SerializedName("following") val following: Boolean,
    @SerializedName("followerCount") val followerCount: Long? = null,
)

data class UserProfileDto(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("followerCount") val followerCount: Long = 0,
    @SerializedName("followingCount") val followingCount: Long = 0,
    @SerializedName("videoCount") val videoCount: Long = 0,
    @SerializedName("likeCount") val likeCount: Long = 0,
    @SerializedName("isFollowing") val isFollowing: Boolean = false,
    @SerializedName("isSelf") val isSelf: Boolean = false,
    @SerializedName("isMe") val isMe: Boolean = false,
)

data class ProfileVideoItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String?,
    @SerializedName("likeCount") val likeCount: Long = 0,
    @SerializedName("durationMs") val durationMs: Long = 0,
    @SerializedName("streamUrl") val streamUrl: String? = null,
    @SerializedName("playbackFormat") val playbackFormat: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("commentCount") val commentCount: Long? = null,
    @SerializedName("shareCount") val shareCount: Long? = null,
    @SerializedName("musicLabel") val musicLabel: String? = null,
    @SerializedName("hashtags") val hashtags: List<String>? = null,
)

data class ProfileVideosDto(
    @SerializedName("items") val items: List<ProfileVideoItemDto>,
)

data class DiscoverHashtagDto(
    @SerializedName("tag") val tag: String,
    @SerializedName("videoCount") val videoCount: Long,
)

data class DiscoverVideoDto(
    @SerializedName("id") val id: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerializedName("likeCount") val likeCount: Long = 0,
    @SerializedName("authorId") val authorId: String? = null,
    @SerializedName("authorName") val authorName: String = "",
    @SerializedName("authorAvatarUrl") val authorAvatarUrl: String? = null,
)

data class DiscoverResponseDto(
    @SerializedName("hashtags") val hashtags: List<DiscoverHashtagDto> = emptyList(),
    @SerializedName("users") val users: List<UserProfileDto> = emptyList(),
    @SerializedName("videos") val videos: List<DiscoverVideoDto> = emptyList(),
)

data class InboxNotificationDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("createdAtLabel") val createdAtLabel: String? = null,
    @SerializedName("videoId") val videoId: String? = null,
    @SerializedName("actorUserId") val actorUserId: String? = null,
)

data class InboxListDto(
    @SerializedName("items") val items: List<InboxNotificationDto>,
    @SerializedName("unreadCount") val unreadCount: Int = 0,
)

data class RegisterDeviceRequestDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("fcmToken") val fcmToken: String,
    @SerializedName("platform") val platform: String = "android",
)

data class UpdateProfileRequestDto(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
)
