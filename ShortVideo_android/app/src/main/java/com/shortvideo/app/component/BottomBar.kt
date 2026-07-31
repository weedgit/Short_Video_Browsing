package com.shortvideo.app.component

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.shortvideo.app.navigation.BottomBarDestination
import com.shortvideo.core.DestinationRoute

@Composable
fun BottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    isAuthenticated: Boolean,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        BottomBarDestination.entries.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            val iconSize = if (destination.emphasized) 34.dp else 24.dp

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!isAuthenticated && destination.route in DestinationRoute.authRequiredRoutes) {
                        navController.navigate(DestinationRoute.authLoginRoute(destination.route)) {
                            launchSingleTop = true
                        }
                        return@NavigationBarItem
                    }

                    navController.navigate(destination.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(DestinationRoute.HOME_ROUTE) {
                            saveState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = destination.titleRes?.let { stringResource(it) },
                        modifier = Modifier
                            .size(iconSize)
                            .offset(y = if (destination.emphasized) (-6).dp else 0.dp),
                        tint = if (destination.emphasized) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 1f else 0.7f)
                        },
                    )
                },
                label = {
                    destination.titleRes?.let {
                        Text(
                            text = stringResource(it),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}
