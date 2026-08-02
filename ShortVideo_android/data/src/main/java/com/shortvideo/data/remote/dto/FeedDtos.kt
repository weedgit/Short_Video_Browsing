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
    @SerializedName("createdAtLabel") val createdAtLabel: String,
)

data class CommentListDto(
    @SerializedName("items") val items: List<CommentDto>,
)

data class CreateCommentRequestDto(
    @SerializedName("text") val text: String,
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
    @SerializedName("likeCount") val likeCount: Long,
    @SerializedName("durationMs") val durationMs: Long,
)

data class ProfileVideosDto(
    @SerializedName("items") val items: List<ProfileVideoItemDto>,
)

data class DiscoverHashtagDto(
    @SerializedName("tag") val tag: String,
    @SerializedName("videoCount") val videoCount: Long,
)

data class DiscoverResponseDto(
    @SerializedName("hashtags") val hashtags: List<DiscoverHashtagDto>,
    @SerializedName("users") val users: List<UserProfileDto> = emptyList(),
    @SerializedName("videos") val videos: List<FeedVideoDto> = emptyList(),
)

data class InboxNotificationDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("createdAtLabel") val createdAtLabel: String,
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
