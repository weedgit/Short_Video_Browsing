package com.shortvideo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.shortvideo.feature.auth.authNavGraph
import com.shortvideo.feature.discover.discoverNavGraph
import com.shortvideo.feature.home.homeNavGraph
import com.shortvideo.feature.inbox.inboxNavGraph
import com.shortvideo.feature.onboarding.accessibilityOnboardingNavGraph
import com.shortvideo.feature.profile.profileNavGraph
import com.shortvideo.feature.settings.settingsNavGraph
import com.shortvideo.feature.upload.uploadNavGraph

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    onAccessibilityOnboardingCompleted: () -> Unit,
    onAuthCompleted: (returnRoute: String?) -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        accessibilityOnboardingNavGraph(onCompleted = onAccessibilityOnboardingCompleted)
        registerFeatureGraphs(navController, onAuthCompleted, onLoggedOut)
    }
}

private fun NavGraphBuilder.registerFeatureGraphs(
    navController: NavHostController,
    onAuthCompleted: (returnRoute: String?) -> Unit,
    onLoggedOut: () -> Unit,
) {
    authNavGraph(
        navController = navController,
        onAuthCompleted = onAuthCompleted,
    )
    settingsNavGraph(
        navController = navController,
        onLoggedOut = onLoggedOut,
    )
    homeNavGraph()
    discoverNavGraph()
    uploadNavGraph()
    inboxNavGraph()
    profileNavGraph(navController)
}
