package com.shortvideo.domain.repository

import com.shortvideo.domain.model.DiscoverHashtag
import com.shortvideo.domain.model.DiscoverTab
import com.shortvideo.domain.model.DiscoverVideo
import com.shortvideo.domain.model.FeedPage
import com.shortvideo.domain.model.FeedVideo
import com.shortvideo.domain.model.InboxNotification
import com.shortvideo.domain.model.ProfileVideoItem
import com.shortvideo.domain.model.UserProfile
import com.shortvideo.domain.model.VideoComment

interface FeedRepository {
    suspend fun loadFeedPage(
        cursor: String? = null,
        limit: Int = 10,
        tab: String = "foryou",
    ): FeedPage
}

interface SocialRepository {
    suspend fun likeVideo(videoId: String): Pair<Boolean, Long>
    suspend fun unlikeVideo(videoId: String): Pair<Boolean, Long>
    suspend fun getComments(videoId: String): List<VideoComment>
    suspend fun postComment(videoId: String, text: String, parentId: String? = null): VideoComment
    suspend fun followUser(userId: String): Boolean
    suspend fun unfollowUser(userId: String): Boolean
    suspend fun saveVideo(videoId: String): Boolean
    suspend fun unsaveVideo(videoId: String): Boolean
    suspend fun reportVideo(videoId: String, title: String, content: String)
    suspend fun reportComment(commentId: String, reason: String)
}

interface ProfileRepository {
    suspend fun getMyProfile(): UserProfile
    suspend fun getProfile(userId: String): UserProfile
    suspend fun getProfileVideos(userId: String): List<ProfileVideoItem>
    suspend fun getProfileFeedVideos(userId: String, limit: Int = 50): List<FeedVideo>
    suspend fun getMyLikedVideos(): List<ProfileVideoItem>
    suspend fun getMyLikedFeedVideos(limit: Int = 50): List<FeedVideo>
    suspend fun getMySavedVideos(): List<ProfileVideoItem>
    suspend fun getMySavedFeedVideos(limit: Int = 50): List<FeedVideo>
    suspend fun updateMyProfile(displayName: String?, bio: String?): UserProfile
    suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String, fileName: String): UserProfile
}

interface DiscoverRepository {
    suspend fun search(query: String?, tab: DiscoverTab = DiscoverTab.VIDEOS): DiscoverResult
}

data class DiscoverResult(
    val hashtags: List<DiscoverHashtag>,
    val users: List<UserProfile>,
    val videos: List<DiscoverVideo>,
)

interface InboxRepository {
    suspend fun getNotifications(): Pair<List<InboxNotification>, Int>
    suspend fun markRead(id: String)
    suspend fun markAllRead()
    suspend fun registerFcmToken(deviceId: String, fcmToken: String)
}
