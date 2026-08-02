package com.shortvideo.feature.profile

import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shortvideo.core.DestinationRoute

@UnstableApi
fun NavGraphBuilder.profileNavGraph(
    navController: NavHostController,
) {
    composable(DestinationRoute.PROFILE_ROUTE) {
        ProfileScreen(
            onNavigateToSettings = {
                navController.navigate(DestinationRoute.SETTINGS_ROUTE)
            },
            onNavigateBack = null,
            onVideoClick = { source, ownerId, videoId ->
                navController.navigate(
                    DestinationRoute.profileVideoFeedRoute(source, ownerId, videoId),
                )
            },
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
            onVideoClick = { source, ownerId, videoId ->
                navController.navigate(
                    DestinationRoute.profileVideoFeedRoute(source, ownerId, videoId),
                )
            },
        )
    }

    composable(
        route = DestinationRoute.PROFILE_VIDEO_FEED_ROUTE,
        arguments = listOf(
            navArgument(DestinationRoute.PROFILE_VIDEO_SOURCE_ARG) {
                type = NavType.StringType
            },
            navArgument(DestinationRoute.PROFILE_VIDEO_OWNER_ARG) {
                type = NavType.StringType
            },
            navArgument(DestinationRoute.PROFILE_VIDEO_ID_ARG) {
                type = NavType.StringType
            },
        ),
    ) {
        ProfileVideoFeedScreen(
            onNavigateBack = { navController.popBackStack() },
            onAvatarClick = { authorId ->
                navController.navigate(DestinationRoute.userProfileRoute(authorId))
            },
        )
    }
}
