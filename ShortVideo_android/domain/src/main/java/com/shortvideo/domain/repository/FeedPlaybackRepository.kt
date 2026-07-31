package com.shortvideo.domain.repository

interface FeedPlaybackRepository {
    suspend fun isMuted(): Boolean
    suspend fun setMuted(muted: Boolean)
    suspend fun getResumePositionMs(videoId: String): Long
    suspend fun setResumePositionMs(videoId: String, positionMs: Long)
}
