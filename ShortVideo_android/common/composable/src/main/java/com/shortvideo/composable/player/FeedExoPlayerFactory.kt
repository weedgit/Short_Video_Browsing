package com.shortvideo.composable.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer

@UnstableApi
object FeedExoPlayerFactory {
    fun createActivePlayer(context: Context): ExoPlayer =
        ExoPlayer.Builder(context.applicationContext)
            .setLoadControl(createActiveLoadControl())
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus= */ true,
            )
            .build()

    fun createPreloadPlayer(context: Context): ExoPlayer =
        ExoPlayer.Builder(context.applicationContext)
            .setLoadControl(createPreloadLoadControl())
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus= */ false,
            )
            .build()

    /**
     * Pool players keep one LoadControl; this is a no-op hint reserved for future
     * per-role tuning without rebuilding the player.
     */
    @Suppress("UNUSED_PARAMETER")
    fun applyLoadControlHint(player: ExoPlayer, forPreload: Boolean) = Unit

    fun createMediaItem(streamUrl: String, playbackFormat: String): MediaItem {
        val builder = MediaItem.Builder().setUri(streamUrl)
        val isHls = playbackFormat.equals("hls", ignoreCase = true) ||
            streamUrl.contains(".m3u8", ignoreCase = true)
        if (isHls) {
            builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }
        return builder.build()
    }

    private fun createActiveLoadControl(): DefaultLoadControl =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                FeedPreloadConfig.ACTIVE_MIN_BUFFER_MS,
                FeedPreloadConfig.ACTIVE_MAX_BUFFER_MS,
                FeedPreloadConfig.BUFFER_FOR_PLAYBACK_MS,
                FeedPreloadConfig.BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

    private fun createPreloadLoadControl(): DefaultLoadControl =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                FeedPreloadConfig.PRELOAD_MIN_BUFFER_MS,
                FeedPreloadConfig.PRELOAD_MAX_BUFFER_MS,
                FeedPreloadConfig.BUFFER_FOR_PLAYBACK_MS,
                FeedPreloadConfig.BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
}
