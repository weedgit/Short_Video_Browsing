package com.shortvideo.app

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shortvideo.app.component.BottomBar
import com.shortvideo.app.navigation.AppNavHost
import com.shortvideo.core.DestinationRoute
import com.shortvideo.theme.ShortVideoTheme

@Composable
fun RootScreen(
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val gate by appViewModel.gateState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                appViewModel.refreshAccessibilityStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!gate.isReady) {
        ShortVideoTheme(darkTheme = gate.isDarkTheme) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        }
        return
    }

    val startDestination = if (gate.shouldShowOnboarding) {
        DestinationRoute.ACCESSIBILITY_ONBOARDING_ROUTE
    } else {
        DestinationRoute.HOME_ROUTE
    }

    LaunchedEffect(gate.shouldShowOnboarding, currentDestination?.route) {
        val route = currentDestination?.route ?: return@LaunchedEffect
        if (gate.shouldShowOnboarding && route != DestinationRoute.ACCESSIBILITY_ONBOARDING_ROUTE) {
            navController.navigate(DestinationRoute.ACCESSIBILITY_ONBOARDING_ROUTE) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        }
    }

    val showBottomBar = gate.canEnterApp && currentDestination?.route in setOf(
        DestinationRoute.HOME_ROUTE,
        DestinationRoute.DISCOVER_ROUTE,
        DestinationRoute.UPLOAD_ROUTE,
        DestinationRoute.INBOX_ROUTE,
        DestinationRoute.PROFILE_ROUTE,
    )

    if (currentDestination?.route == DestinationRoute.HOME_ROUTE) {
        BackHandler {
            (context as? Activity)?.finish()
        }
    }

    if (currentDestination?.route == DestinationRoute.ACCESSIBILITY_ONBOARDING_ROUTE) {
        BackHandler {
            (context as? Activity)?.finish()
        }
    }

    if (DestinationRoute.isAuthRoute(currentDestination?.route)) {
        BackHandler {
            navController.navigate(DestinationRoute.HOME_ROUTE) {
                popUpTo(DestinationRoute.HOME_ROUTE) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    ShortVideoTheme(darkTheme = gate.isDarkTheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                if (showBottomBar) {
                    BottomBar(
                        navController = navController,
                        currentDestination = currentDestination,
                        isAuthenticated = gate.isAuthenticated,
                    )
                }
            },
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                startDestination = startDestination,
                onAccessibilityOnboardingCompleted = {
                    navController.navigate(DestinationRoute.HOME_ROUTE) {
                        popUpTo(DestinationRoute.ACCESSIBILITY_ONBOARDING_ROUTE) {
                            inclusive = true
                        }
                    }
                },
                onAuthCompleted = { returnRoute ->
                    val destination = returnRoute?.takeIf { it.isNotBlank() }
                        ?: DestinationRoute.HOME_ROUTE
                    navController.navigate(destination) {
                        popUpTo(DestinationRoute.AUTH_LOGIN_ROUTE) {
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onLoggedOut = {
                    navController.navigate(DestinationRoute.HOME_ROUTE) {
                        popUpTo(DestinationRoute.HOME_ROUTE) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
