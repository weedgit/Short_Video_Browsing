package com.shortvideo.domain.repository

import com.shortvideo.domain.model.FeedPage
import com.shortvideo.domain.model.PlaybackEvent

interface FeedRepository {
    suspend fun loadFeedPage(cursor: String? = null, limit: Int = 10): FeedPage
}

interface PlaybackEventRepository {
    suspend fun enqueue(event: PlaybackEvent)
    suspend fun flush()
}
