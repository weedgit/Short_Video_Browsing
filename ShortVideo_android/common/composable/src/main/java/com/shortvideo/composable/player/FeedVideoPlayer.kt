package com.shortvideo.composable.player

import android.os.SystemClock
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.shortvideo.composable.feed.FeedPlaybackErrorOverlay
import com.shortvideo.composable.util.clampPlaybackPosition
import com.shortvideo.theme.OverlayWhiteColor
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@UnstableApi
@Composable
fun FeedVideoPlayer(
    streamUrl: String,
    isActive: Boolean,
    isPreloading: Boolean,
    isMuted: Boolean,
    initialPositionMs: Long,
    durationMs: Long,
    seekPreviewPositionMs: Long?,
    onPositionChanged: (Long) -> Unit,
    playerPool: FeedPlayerPool? = null,
    playbackFormat: String = "mp4",
    thumbnailUrl: String? = null,
    onDurationChanged: (Long) -> Unit = {},
    onPlaybackStarted: () -> Unit = {},
    onFirstFrame: (ttffMs: Long) -> Unit = {},
    onDoubleTap: ((offsetX: Float, offsetY: Float) -> Unit)? = null,
    onPreloadProgress: (bufferedAheadMs: Long, isComplete: Boolean) -> Unit = { _, _ -> },
    canSkipToNext: Boolean = false,
    onSkipToNext: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!isActive && !isPreloading) return

    val context = LocalContext.current
    var showPauseIcon by remember { mutableStateOf(false) }
    var hasPlaybackError by remember(streamUrl) { mutableStateOf(false) }
    var isBuffering by remember(streamUrl) { mutableStateOf(true) }
    var retryCount by remember(streamUrl) { mutableIntStateOf(0) }
    var hasStarted by remember(streamUrl) { mutableStateOf(false) }
    var showPoster by remember(streamUrl) { mutableStateOf(true) }
    val prepareStartedAtMs = remember(streamUrl, retryCount) { SystemClock.elapsedRealtime() }
    val onFirstFrameState = rememberUpdatedState(onFirstFrame)
    val onPlaybackStartedState = rememberUpdatedState(onPlaybackStarted)

    val exoPlayer = remember(streamUrl, retryCount, isPreloading, playerPool) {
        val pooled = playerPool != null
        val player = if (pooled) {
            playerPool!!.acquire(forPreload = isPreloading)
        } else if (isPreloading) {
            FeedExoPlayerFactory.createPreloadPlayer(context)
        } else {
            FeedExoPlayerFactory.createActivePlayer(context)
        }

        player.apply {
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (isMuted) 0f else 1f
            setMediaItem(FeedExoPlayerFactory.createMediaItem(streamUrl, playbackFormat))
            playWhenReady = isActive && !isPreloading
            if (initialPositionMs > 0L && isActive) {
                seekTo(initialPositionMs)
            }
            prepare()
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(isActive, isPreloading) {
        exoPlayer.playWhenReady = isActive && !isPreloading
        if (isPreloading) {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(exoPlayer, isPreloading) {
        if (!isPreloading) return@LaunchedEffect

        while (currentCoroutineContext().isActive) {
            val bufferedAheadMs = FeedPreloadConfig.bufferedAheadMs(
                bufferedPositionMs = exoPlayer.bufferedPosition,
                currentPositionMs = exoPlayer.currentPosition,
            )
            val isComplete = FeedPreloadConfig.isPreloadComplete(bufferedAheadMs) ||
                exoPlayer.playbackState == Player.STATE_ENDED

            onPreloadProgress(bufferedAheadMs, isComplete)

            if (isComplete || FeedPreloadConfig.isPreloadSatisfied(bufferedAheadMs)) {
                exoPlayer.pause()
            }

            if (isComplete) {
                break
            }

            delay(FeedPreloadConfig.PRELOAD_POLL_INTERVAL_MS)
        }
    }

    DisposableEffect(exoPlayer, playerPool) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING

                when (playbackState) {
                    Player.STATE_READY -> {
                        hasPlaybackError = false
                        showPoster = false
                        val duration = exoPlayer.duration
                        if (duration > 0L) {
                            onDurationChanged(duration)
                        }
                        onPositionChanged(exoPlayer.currentPosition)
                        if (isActive && exoPlayer.isPlaying && !hasStarted) {
                            hasStarted = true
                            val ttff = SystemClock.elapsedRealtime() - prepareStartedAtMs
                            onFirstFrameState.value(ttff)
                            onPlaybackStartedState.value()
                        }
                    }
                    Player.STATE_IDLE -> {
                        if (exoPlayer.playerError != null) {
                            hasPlaybackError = true
                        }
                    }
                    else -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                hasPlaybackError = true
                isBuffering = false
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                onPositionChanged(exoPlayer.currentPosition)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && isActive && !hasStarted) {
                    hasStarted = true
                    showPoster = false
                    val ttff = SystemClock.elapsedRealtime() - prepareStartedAtMs
                    onFirstFrameState.value(ttff)
                    onPlaybackStartedState.value()
                }
            }

            override fun onRenderedFirstFrame() {
                showPoster = false
                if (isActive && !hasStarted) {
                    hasStarted = true
                    val ttff = SystemClock.elapsedRealtime() - prepareStartedAtMs
                    onFirstFrameState.value(ttff)
                    onPlaybackStartedState.value()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            if (playerPool != null) {
                playerPool.release(exoPlayer)
            } else {
                exoPlayer.release()
            }
        }
    }

    LaunchedEffect(exoPlayer, isActive) {
        if (!isActive) return@LaunchedEffect

        while (currentCoroutineContext().isActive) {
            val duration = exoPlayer.duration
            if (duration > 0L) {
                onDurationChanged(duration)
            }
            if (!hasPlaybackError) {
                onPositionChanged(exoPlayer.currentPosition)
            }
            delay(200)
        }
    }

    DisposableEffect(seekPreviewPositionMs) {
        if (seekPreviewPositionMs != null && isActive) {
            val effectiveDurationMs = exoPlayer.duration.takeIf { it > 0L } ?: durationMs
            val target = clampPlaybackPosition(seekPreviewPositionMs, effectiveDurationMs)
            exoPlayer.seekTo(target)
            onPositionChanged(target)
        }
        onDispose { }
    }

    val lifecycleOwner by rememberUpdatedState(LocalLifecycleOwner.current)
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    exoPlayer.pause()
                    showPauseIcon = false
                }
                Lifecycle.Event.ON_START -> {
                    if (isActive && exoPlayer.playWhenReady && !hasPlaybackError) {
                        exoPlayer.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (isPreloading) return

    val playerView = remember(streamUrl, retryCount) {
        PlayerView(context).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            player = exoPlayer
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { playerView },
            update = { view ->
                if (view.player != exoPlayer) {
                    view.player = exoPlayer
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(streamUrl, hasPlaybackError, onDoubleTap) {
                    if (hasPlaybackError) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            onDoubleTap?.invoke(offset.x, offset.y)
                        },
                        onTap = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                showPauseIcon = true
                            } else {
                                exoPlayer.play()
                                showPauseIcon = false
                            }
                        },
                    )
                },
        )

        if (showPoster && !thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            )
        }

        if ((isBuffering || showPoster) && !hasPlaybackError) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = OverlayWhiteColor,
            )
        }

        if (showPauseIcon && !exoPlayer.isPlaying && !hasPlaybackError) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Paused",
                tint = OverlayWhiteColor,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
            )
        }

        if (hasPlaybackError) {
            FeedPlaybackErrorOverlay(
                message = "Unable to load this video. Check your network connection.",
                onRetry = {
                    hasPlaybackError = false
                    isBuffering = true
                    showPoster = true
                    retryCount++
                },
                onSkipToNext = if (canSkipToNext) onSkipToNext else null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
