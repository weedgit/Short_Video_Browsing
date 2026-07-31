package com.shortvideo.domain.repository

import com.shortvideo.domain.model.PlaybackEvent

interface PlaybackEventRepository {
    suspend fun enqueue(event: PlaybackEvent)
    suspend fun flush()
}
