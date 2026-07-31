package com.shortvideo.composable.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shortvideo.composable.util.formatDuration

@Composable
fun FeedPlaybackControls(
    currentPositionMs: Long,
    durationMs: Long,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(
                imageVector = if (isMuted) {
                    Icons.AutoMirrored.Filled.VolumeOff
                } else {
                    Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = Color.White,
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = 48.dp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.25f),
        )

        Text(
            text = "${formatDuration(currentPositionMs)} / ${formatDuration(durationMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(end = 48.dp, top = 8.dp),
        )
    }
}
