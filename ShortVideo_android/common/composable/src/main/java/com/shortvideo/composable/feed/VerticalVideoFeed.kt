package com.shortvideo.composable.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.shortvideo.composable.player.FeedVideoPlayer
import com.shortvideo.composable.util.clampPlaybackPosition
import com.shortvideo.composable.util.seekDeltaFromDrag
import com.shortvideo.domain.model.FeedVideo
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private enum class GestureLock { None, Vertical, Horizontal }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalVideoFeed(
    videos: List<FeedVideo>,
    isMuted: Boolean,
    resumePositionsMs: Map<String, Long>,
    onToggleMute: () -> Unit,
    onNearEnd: (Int) -> Unit,
    onActiveVideoChanged: (FeedVideo, Long) -> Unit,
    onVideoStarted: (FeedVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (videos.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { videos.size })
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val directionThresholdPx = with(density) { 16.dp.toPx() }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                onNearEnd(pageIndex)
                val video = videos.getOrNull(pageIndex) ?: return@collect
                onVideoStarted(video)
            }
    }

    VerticalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = modifier.fillMaxSize(),
    ) { pageIndex ->
        val video = videos[pageIndex]
        val isActive = pagerState.settledPage == pageIndex
        val isPreloading = pageIndex == pagerState.settledPage + 1

        var currentPositionMs by remember(video.id) {
            mutableLongStateOf(resumePositionsMs[video.id] ?: 0L)
        }
        var playbackDurationMs by remember(video.id) { mutableLongStateOf(video.durationMs) }
        var seekPreviewPositionMs by remember(video.id) { mutableStateOf<Long?>(null) }
        var seekPreviewDeltaMs by remember(video.id) { mutableLongStateOf(0L) }
        var isSeeking by remember(video.id) { mutableStateOf(false) }
        var dragStartPositionMs by remember(video.id) { mutableLongStateOf(0L) }

        LaunchedEffect(isActive, currentPositionMs) {
            if (isActive) {
                onActiveVideoChanged(video, currentPositionMs)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(video.id, isActive) {
                    if (!isActive) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalX = 0f
                        var totalY = 0f
                        var lock = GestureLock.None

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break

                            val delta = change.positionChange()
                            totalX += delta.x
                            totalY += delta.y

                            if (lock == GestureLock.None) {
                                if (abs(totalX) >= directionThresholdPx ||
                                    abs(totalY) >= directionThresholdPx
                                ) {
                                    lock = if (abs(totalX) > abs(totalY)) {
                                        GestureLock.Horizontal
                                    } else {
                                        GestureLock.Vertical
                                    }
                                    if (lock == GestureLock.Horizontal) {
                                        dragStartPositionMs = currentPositionMs
                                    }
                                }
                            }

                            if (lock == GestureLock.Horizontal) {
                                change.consume()
                                val deltaMs = seekDeltaFromDrag(
                                    dragX = totalX,
                                    screenWidthPx = size.width.toFloat(),
                                    durationMs = playbackDurationMs,
                                )
                                val preview = clampPlaybackPosition(
                                    dragStartPositionMs + deltaMs,
                                    playbackDurationMs,
                                )
                                seekPreviewDeltaMs = preview - dragStartPositionMs
                                seekPreviewPositionMs = preview
                                isSeeking = true
                            }
                        } while (event.changes.any { it.pressed })

                        if (lock == GestureLock.Horizontal) {
                            val finalPosition = clampPlaybackPosition(
                                seekPreviewPositionMs ?: currentPositionMs,
                                playbackDurationMs,
                            )
                            currentPositionMs = finalPosition
                            isSeeking = false
                            seekPreviewDeltaMs = 0L
                            seekPreviewPositionMs = finalPosition
                        }
                    }
                },
        ) {
            FeedVideoPlayer(
                streamUrl = video.streamUrl,
                isActive = isActive,
                isPreloading = isPreloading,
                isMuted = isMuted,
                initialPositionMs = resumePositionsMs[video.id] ?: 0L,
                durationMs = playbackDurationMs,
                seekPreviewPositionMs = seekPreviewPositionMs,
                onPositionChanged = { positionMs ->
                    if (!isSeeking) {
                        currentPositionMs = positionMs
                    }
                },
                onDurationChanged = { durationMs ->
                    playbackDurationMs = durationMs
                },
                onPlaybackStarted = { onVideoStarted(video) },
                canSkipToNext = pageIndex < videos.lastIndex,
                onSkipToNext = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(min(pageIndex + 1, videos.lastIndex))
                    }
                },
            )

            FeedMetadataFooter(
                video = video,
                modifier = Modifier.align(Alignment.BottomStart),
            )

            if (isActive) {
                FeedPlaybackControls(
                    currentPositionMs = currentPositionMs,
                    durationMs = playbackDurationMs,
                    isMuted = isMuted,
                    onToggleMute = onToggleMute,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }

            if (isSeeking) {
                SeekTimeOverlay(
                    previewDeltaMs = seekPreviewDeltaMs,
                    previewPositionMs = seekPreviewPositionMs ?: currentPositionMs,
                    durationMs = playbackDurationMs,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
