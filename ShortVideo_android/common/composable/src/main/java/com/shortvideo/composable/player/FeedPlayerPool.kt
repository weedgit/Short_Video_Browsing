package com.shortvideo.composable.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.util.ArrayDeque

/**
 * Reuses 2–3 ExoPlayer instances across feed pages to cut recreate cost on swipe.
 */
@UnstableApi
class FeedPlayerPool(
    private val context: Context,
    private val capacity: Int = FeedPreloadConfig.PLAYER_POOL_SIZE,
) {
    private val available = ArrayDeque<ExoPlayer>(capacity)
    private val inUse = mutableSetOf<ExoPlayer>()

    init {
        repeat(capacity) {
            available.addLast(FeedExoPlayerFactory.createActivePlayer(context.applicationContext))
        }
    }

    fun acquire(forPreload: Boolean): ExoPlayer {
        val player = available.removeFirstOrNull()
            ?: FeedExoPlayerFactory.createActivePlayer(context.applicationContext)
        inUse.add(player)
        FeedExoPlayerFactory.applyLoadControlHint(player, forPreload)
        return player
    }

    fun release(player: ExoPlayer) {
        if (!inUse.remove(player)) return
        runCatching {
            player.stop()
            player.clearMediaItems()
            player.playWhenReady = false
            player.volume = 1f
        }
        if (available.size < capacity) {
            available.addLast(player)
        } else {
            player.release()
        }
    }

    fun releaseAll() {
        (available + inUse).forEach { player ->
            runCatching { player.release() }
        }
        available.clear()
        inUse.clear()
    }
}
