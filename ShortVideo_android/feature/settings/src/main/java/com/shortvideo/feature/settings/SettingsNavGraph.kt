package com.shortvideo.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.shortvideo.core.DestinationRoute

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
    onLoggedOut: () -> Unit,
) {
    composable(DestinationRoute.SETTINGS_ROUTE) {
        SettingsScreen(
            onNavigateToPasswordReset = {
                navController.navigate(DestinationRoute.AUTH_PASSWORD_RESET_ROUTE)
            },
            onLoggedOut = onLoggedOut,
        )
    }
}
