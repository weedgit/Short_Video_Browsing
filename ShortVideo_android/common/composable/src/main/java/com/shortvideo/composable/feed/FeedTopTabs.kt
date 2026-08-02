package com.shortvideo.composable.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FeedTab { Following, ForYou }

/** Scrim opacity over the video behind Following / For You / Search. */
private const val TopBarScrimAlpha = 0.55f

@Composable
fun FeedTopTabs(
    selectedTab: FeedTab,
    onTabSelected: (FeedTab) -> Unit,
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = TopBarScrimAlpha),
                        Color.Black.copy(alpha = TopBarScrimAlpha * 0.65f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(start = 4.dp, end = 4.dp, top = 0.dp, bottom = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeedTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    TextButton(onClick = { onTabSelected(tab) }) {
                        Text(
                            text = when (tab) {
                                FeedTab.Following -> "Following"
                                FeedTab.ForYou -> "For You"
                            },
                            color = if (selected) {
                                Color.White
                            } else {
                                Color.White.copy(alpha = 0.78f)
                            },
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 17.sp,
                        )
                    }
                }
            }
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                )
            }
        }
    }
}
