package com.shortvideo.composable.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FeedTab { Following, ForYou }

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
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp,
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
