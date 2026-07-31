package com.shortvideo.feature.discover

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.shortvideo.composable.SearchWireframeScreen
import com.shortvideo.core.DestinationRoute

@Composable
fun DiscoverScreen() {
    SearchWireframeScreen()
}

fun NavGraphBuilder.discoverNavGraph() {
    composable(DestinationRoute.DISCOVER_ROUTE) {
        DiscoverScreen()
    }
}
