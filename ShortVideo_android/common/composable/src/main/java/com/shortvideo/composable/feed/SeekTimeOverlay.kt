package com.shortvideo.composable.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shortvideo.composable.util.formatDuration

@Composable
fun SeekTimeOverlay(
    previewDeltaMs: Long,
    previewPositionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    val sign = if (previewDeltaMs >= 0) "+" else "-"
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "$sign${formatDuration(kotlin.math.abs(previewDeltaMs))}",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Text(
            text = "${formatDuration(previewPositionMs)} / ${formatDuration(durationMs)}",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}
