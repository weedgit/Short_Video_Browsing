package com.shortvideo.data.repository

import com.shortvideo.data.preferences.FeedPlaybackPreferences
import com.shortvideo.domain.repository.FeedPlaybackRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedPlaybackRepositoryImpl @Inject constructor(
    private val feedPlaybackPreferences: FeedPlaybackPreferences,
) : FeedPlaybackRepository {
    override suspend fun isMuted(): Boolean = feedPlaybackPreferences.isMuted()

    override suspend fun setMuted(muted: Boolean) {
        feedPlaybackPreferences.setMuted(muted)
    }

    override suspend fun getResumePositionMs(videoId: String): Long =
        feedPlaybackPreferences.getResumePositionMs(videoId)

    override suspend fun setResumePositionMs(videoId: String, positionMs: Long) {
        feedPlaybackPreferences.setResumePositionMs(videoId, positionMs)
    }
}
