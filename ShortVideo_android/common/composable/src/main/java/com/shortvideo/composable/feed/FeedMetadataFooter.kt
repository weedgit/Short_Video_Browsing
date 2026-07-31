package com.shortvideo.composable.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shortvideo.domain.model.FeedVideo

@Composable
fun FeedMetadataFooter(
    video: FeedVideo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                ),
            )
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 32.dp),
    ) {
        Text(
            text = video.authorName,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
        )
        Text(
            text = video.description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        val tags = video.hashtags.joinToString(" ")
        if (tags.isNotBlank()) {
            Text(
                text = tags,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        val meta = buildList {
            video.category?.let { add(it) }
            add(video.uploadedAtLabel)
        }.joinToString(" · ")
        Text(
            text = meta,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun SeekTimeOverlay(
    previewDeltaMs: Long,
    previewPositionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(vertical = 24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        val deltaLabel = com.shortvideo.composable.util.formatSeekDelta(previewDeltaMs)
        val current = com.shortvideo.composable.util.formatDuration(previewPositionMs)
        val total = com.shortvideo.composable.util.formatDuration(durationMs)
        Text(
            text = "$deltaLabel    $current / $total",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}
