package com.shortvideo.feature.profile

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shortvideo.core.DestinationRoute

fun NavGraphBuilder.profileNavGraph(
    navController: NavHostController,
) {
    composable(DestinationRoute.PROFILE_ROUTE) {
        ProfileScreen(
            onNavigateToSettings = {
                navController.navigate(DestinationRoute.SETTINGS_ROUTE)
            },
            onNavigateBack = null,
        )
    }

    composable(
        route = DestinationRoute.USER_PROFILE_ROUTE,
        arguments = listOf(
            navArgument(DestinationRoute.USER_ID_ARG) {
                type = NavType.StringType
            },
        ),
    ) {
        ProfileScreen(
            onNavigateToSettings = {
                navController.navigate(DestinationRoute.SETTINGS_ROUTE)
            },
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
