package com.shortvideo.composable.util

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun formatDuration(ms: Long): String {
    val totalSeconds = max(0L, ms) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

fun formatSeekDelta(deltaMs: Long): String {
    val sign = if (deltaMs >= 0) "+" else "-"
    return "$sign${formatDuration(abs(deltaMs))}"
}

fun clampPlaybackPosition(positionMs: Long, durationMs: Long): Long {
    if (durationMs <= 0L) return 0L
    return min(max(0L, positionMs), durationMs)
}

fun seekDeltaFromDrag(
    dragX: Float,
    screenWidthPx: Float,
    durationMs: Long,
): Long {
    if (screenWidthPx <= 0f || durationMs <= 0L) return 0L
    val ratio = dragX / screenWidthPx
    val maxSeekMs = min(60_000L, durationMs)
    return (ratio * maxSeekMs).toLong()
}
