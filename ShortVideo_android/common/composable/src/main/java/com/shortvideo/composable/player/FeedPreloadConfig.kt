package com.shortvideo.composable.player

/**
 * Preload next clips (N+1, N+2) with bounded buffers so long videos are not fully downloaded.
 */
object FeedPreloadConfig {
    const val PLAYER_POOL_SIZE = 3
    const val PRELOAD_AHEAD_COUNT = 2

    const val PRELOAD_MIN_BUFFER_MS = 5_000
    const val PRELOAD_MAX_BUFFER_MS = 12_000

    /** Active playback keeps a modest buffer so long videos are not fully downloaded. */
    const val ACTIVE_MIN_BUFFER_MS = 12_000
    const val ACTIVE_MAX_BUFFER_MS = 28_000

    const val BUFFER_FOR_PLAYBACK_MS = 800
    const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 1_500

    const val PRELOAD_POLL_INTERVAL_MS = 250L

    fun bufferedAheadMs(bufferedPositionMs: Long, currentPositionMs: Long): Long =
        (bufferedPositionMs - currentPositionMs).coerceAtLeast(0L)

    fun isPreloadSatisfied(bufferedAheadMs: Long): Boolean =
        bufferedAheadMs >= PRELOAD_MIN_BUFFER_MS

    fun isPreloadComplete(bufferedAheadMs: Long): Boolean =
        bufferedAheadMs >= PRELOAD_MAX_BUFFER_MS
}
