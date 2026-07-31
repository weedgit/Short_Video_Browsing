package com.shortvideo.feature.inbox

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.shortvideo.composable.WireframeScreen
import com.shortvideo.core.DestinationRoute

@Composable
fun InboxScreen() {
    WireframeScreen(
        title = "Inbox",
        sections = listOf(
            "Server notifications only (no chat)",
            "Upload complete / failed",
            "Unread badge on tab",
            "Read / read-all actions (Phase 5)",
        ),
    )
}

fun NavGraphBuilder.inboxNavGraph() {
    composable(DestinationRoute.INBOX_ROUTE) {
        InboxScreen()
    }
}
