package com.shortvideo.domain.repository

import com.shortvideo.domain.model.DiscoverHashtag
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
    suspend fun postComment(videoId: String, text: String): VideoComment
    suspend fun followUser(userId: String): Boolean
    suspend fun unfollowUser(userId: String): Boolean
    suspend fun saveVideo(videoId: String): Boolean
    suspend fun unsaveVideo(videoId: String): Boolean
}

interface ProfileRepository {
    suspend fun getMyProfile(): UserProfile
    suspend fun getProfile(userId: String): UserProfile
    suspend fun getProfileVideos(userId: String): List<ProfileVideoItem>
}

interface DiscoverRepository {
    suspend fun search(query: String?): DiscoverResult
}

data class DiscoverResult(
    val hashtags: List<DiscoverHashtag>,
    val users: List<UserProfile>,
    val videos: List<FeedVideo>,
)

interface InboxRepository {
    suspend fun getNotifications(): Pair<List<InboxNotification>, Int>
    suspend fun markRead(id: String)
    suspend fun markAllRead()
    suspend fun registerFcmToken(deviceId: String, fcmToken: String)
}
