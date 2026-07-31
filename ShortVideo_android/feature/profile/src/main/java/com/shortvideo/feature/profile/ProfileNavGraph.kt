package com.shortvideo.feature.profile

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.shortvideo.core.DestinationRoute

fun NavGraphBuilder.profileNavGraph(
    navController: NavHostController,
) {
    composable(DestinationRoute.PROFILE_ROUTE) {
        ProfileScreen(
            onNavigateToSettings = {
                navController.navigate(DestinationRoute.SETTINGS_ROUTE)
            },
        )
    }
}
