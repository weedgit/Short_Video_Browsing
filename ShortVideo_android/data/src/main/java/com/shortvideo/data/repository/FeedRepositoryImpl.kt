package com.shortvideo.data.repository

import com.shortvideo.data.mapper.toDomain
import com.shortvideo.data.mapper.toMockPage
import com.shortvideo.data.remote.FeedApi
import com.shortvideo.data.remote.dto.PlaybackBatchRequestDto
import com.shortvideo.data.remote.dto.PlaybackEventRequestDto
import com.shortvideo.data.source.MockFeedDataSource
import com.shortvideo.domain.model.FeedPage
import com.shortvideo.domain.model.PlaybackEvent
import com.shortvideo.domain.repository.FeedRepository
import com.shortvideo.domain.repository.PlaybackEventRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class FeedRepositoryImpl @Inject constructor(
    private val feedApi: FeedApi,
) : FeedRepository {
    override suspend fun loadFeedPage(cursor: String?, limit: Int): FeedPage {
        return try {
            val page = feedApi.getFeed(cursor = cursor, limit = limit).data?.toDomain()
            when {
                page == null -> MockFeedDataSource.getVideos().toMockPage()
                cursor == null && page.videos.isEmpty() -> MockFeedDataSource.getVideos().toMockPage()
                else -> page
            }
        } catch (_: Exception) {
            MockFeedDataSource.getVideos().toMockPage()
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
