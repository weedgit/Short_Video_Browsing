package com.shortvideo.data.repository

import com.shortvideo.data.mapper.toDomain
import com.shortvideo.data.mapper.toMockPage
import com.shortvideo.data.remote.DiscoverApi
import com.shortvideo.data.remote.FeedApi
import com.shortvideo.data.remote.InboxApi
import com.shortvideo.data.remote.ProfileApi
import com.shortvideo.data.remote.SocialApi
import com.shortvideo.data.remote.dto.CreateCommentRequestDto
import com.shortvideo.data.remote.dto.CreateReportRequestDto
import com.shortvideo.data.remote.dto.PlaybackBatchRequestDto
import com.shortvideo.data.remote.dto.PlaybackEventRequestDto
import com.shortvideo.data.remote.dto.RegisterDeviceRequestDto
import com.shortvideo.data.remote.dto.UpdateProfileRequestDto
import com.shortvideo.data.source.MockFeedDataSource
import com.shortvideo.domain.model.DiscoverTab
import com.shortvideo.domain.model.FeedPage
import com.shortvideo.domain.model.InboxNotification
import com.shortvideo.domain.model.PlaybackEvent
import com.shortvideo.domain.model.ProfileVideoItem
import com.shortvideo.domain.model.UserProfile
import com.shortvideo.domain.model.VideoComment
import com.shortvideo.domain.repository.DiscoverRepository
import com.shortvideo.domain.repository.DiscoverResult
import com.shortvideo.domain.repository.FeedRepository
import com.shortvideo.domain.repository.InboxRepository
import com.shortvideo.domain.repository.PlaybackEventRepository
import com.shortvideo.domain.repository.ProfileRepository
import com.shortvideo.domain.repository.SocialRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class FeedRepositoryImpl @Inject constructor(
    private val feedApi: FeedApi,
) : FeedRepository {
    override suspend fun loadFeedPage(cursor: String?, limit: Int, tab: String): FeedPage {
        return try {
            val page = feedApi.getFeed(cursor = cursor, limit = limit, tab = tab).data?.toDomain()
            when {
                page == null -> MockFeedDataSource.getVideos().toMockPage()
                cursor == null && page.videos.isEmpty() && tab == "foryou" ->
                    MockFeedDataSource.getVideos().toMockPage()
                else -> page
            }
        } catch (_: Exception) {
            if (tab == "foryou") MockFeedDataSource.getVideos().toMockPage()
            else FeedPage(emptyList(), null, false)
        }
    }
}

@Singleton
class PlaybackEventRepositoryImpl @Inject constructor(
    private val feedApi: FeedApi,
) : PlaybackEventRepository {
    private val mutex = Mutex()
    private val pendingEvents = mutableListOf<PlaybackEvent>()

    override suspend fun enqueue(event: PlaybackEvent) {
        mutex.withLock {
            pendingEvents.add(event)
            if (pendingEvents.size >= 10) {
                flushLocked()
            }
        }
    }

    override suspend fun flush() {
        mutex.withLock {
            flushLocked()
        }
    }

    private suspend fun flushLocked() {
        if (pendingEvents.isEmpty()) return

        val batch = pendingEvents.toList()
        pendingEvents.clear()

        try {
            feedApi.postPlaybackEventsBatch(
                PlaybackBatchRequestDto(
                    events = batch.map { event ->
                        PlaybackEventRequestDto(
                            videoId = event.videoId,
                            eventType = event.eventType,
                            positionMs = event.positionMs,
                            occurredAt = Instant.now().toString(),
                        )
                    },
                ),
            )
        } catch (_: Exception) {
            pendingEvents.addAll(0, batch.take(50))
        }
    }
}

@Singleton
class SocialRepositoryImpl @Inject constructor(
    private val socialApi: SocialApi,
) : SocialRepository {
    override suspend fun likeVideo(videoId: String): Pair<Boolean, Long> {
        val data = socialApi.likeVideo(videoId).data ?: return true to 0L
        return data.liked to data.likeCount
    }

    override suspend fun unlikeVideo(videoId: String): Pair<Boolean, Long> {
        val data = socialApi.unlikeVideo(videoId).data ?: return false to 0L
        return data.liked to data.likeCount
    }

    override suspend fun getComments(videoId: String): List<VideoComment> =
        socialApi.getComments(videoId).data?.items?.map { it.toDomain() }.orEmpty()

    override suspend fun postComment(
        videoId: String,
        text: String,
        parentId: String?,
    ): VideoComment =
        socialApi.postComment(
            videoId,
            CreateCommentRequestDto(text = text, parentId = parentId),
        ).data!!.toDomain()

    override suspend fun followUser(userId: String): Boolean =
        socialApi.followUser(userId).data?.following ?: true

    override suspend fun unfollowUser(userId: String): Boolean =
        socialApi.unfollowUser(userId).data?.following ?: false

    override suspend fun saveVideo(videoId: String): Boolean =
        socialApi.saveVideo(videoId).data?.get("saved") ?: true

    override suspend fun unsaveVideo(videoId: String): Boolean =
        socialApi.unsaveVideo(videoId).data?.get("saved") ?: false

    override suspend fun reportComment(commentId: String, reason: String) {
        socialApi.submitReport(
            CreateReportRequestDto(
                targetType = "COMMENT",
                targetId = commentId,
                reason = reason,
            ),
        )
    }
}

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi,
) : ProfileRepository {
    override suspend fun getMyProfile(): UserProfile =
        profileApi.getMyProfile().data!!.toDomain()

    override suspend fun getProfile(userId: String): UserProfile =
        profileApi.getProfile(userId).data!!.toDomain()

    override suspend fun getProfileVideos(userId: String): List<ProfileVideoItem> =
        profileApi.getProfileVideos(userId).data?.items?.map { it.toDomain() }.orEmpty()

    override suspend fun getMyLikedVideos(): List<ProfileVideoItem> =
        profileApi.getMyLikedVideos().data?.items?.map { it.toDomain() }.orEmpty()

    override suspend fun getMySavedVideos(): List<ProfileVideoItem> =
        profileApi.getMySavedVideos().data?.items?.map { it.toDomain() }.orEmpty()

    override suspend fun updateMyProfile(displayName: String?, bio: String?): UserProfile =
        profileApi.updateMyProfile(
            UpdateProfileRequestDto(
                displayName = displayName,
                bio = bio,
            ),
        ).data!!.toDomain()

    override suspend fun uploadAvatar(
        imageBytes: ByteArray,
        mimeType: String,
        fileName: String,
    ): UserProfile {
        val body = imageBytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("avatar", fileName, body)
        return profileApi.uploadAvatar(part).data!!.toDomain()
    }
}

@Singleton
class DiscoverRepositoryImpl @Inject constructor(
    private val discoverApi: DiscoverApi,
) : DiscoverRepository {
    override suspend fun search(query: String?, tab: DiscoverTab): DiscoverResult {
        val data = discoverApi.discover(query = query, tab = tab.apiValue).data
        return DiscoverResult(
            hashtags = data?.hashtags?.map { it.toDomain() }.orEmpty(),
            users = data?.users?.map { it.toDomain() }.orEmpty(),
            videos = data?.videos?.map { it.toDomain() }.orEmpty(),
        )
    }
}

@Singleton
class InboxRepositoryImpl @Inject constructor(
    private val inboxApi: InboxApi,
) : InboxRepository {
    override suspend fun getNotifications(): Pair<List<InboxNotification>, Int> {
        val data = inboxApi.getInbox().data
        return (data?.items?.map { it.toDomain() }.orEmpty()) to (data?.unreadCount ?: 0)
    }

    override suspend fun markRead(id: String) {
        runCatching { inboxApi.markRead(id) }
    }

    override suspend fun markAllRead() {
        runCatching { inboxApi.markAllRead() }
    }

    override suspend fun registerFcmToken(deviceId: String, fcmToken: String) {
        runCatching {
            inboxApi.registerFcm(
                RegisterDeviceRequestDto(deviceId = deviceId, fcmToken = fcmToken),
            )
        }
    }
}
