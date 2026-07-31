package com.shortvideo.app.component

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.shortvideo.app.navigation.BottomBarDestination
import com.shortvideo.core.DestinationRoute
import com.shortvideo.theme.Black
import com.shortvideo.theme.PrimaryColor

@Composable
fun BottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    isAuthenticated: Boolean,
) {
    val onHome = currentDestination?.route == DestinationRoute.HOME_ROUTE
    NavigationBar(
        containerColor = if (onHome) Black else MaterialThemeSurface(),
        contentColor = Color.White,
    ) {
        BottomBarDestination.entries.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            val iconSize = if (destination.emphasized) 34.dp else 24.dp
            val inactive = if (onHome) Color.White.copy(alpha = 0.55f) else Color.Unspecified

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
                        tint = when {
                            destination.emphasized -> PrimaryColor
                            selected -> Color.White
                            onHome -> inactive
                            else -> Color.Unspecified
                        },
                    )
                },
                label = {
                    destination.titleRes?.let {
                        Text(
                            text = stringResource(it),
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = if (onHome) {
                                if (selected) Color.White else inactive
                            } else {
                                Color.Unspecified
                            },
                        )
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = Color.White,
                    unselectedIconColor = if (onHome) inactive else Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = if (onHome) inactive else Color.Gray,
                ),
            )
        }
    }
}

@Composable
private fun MaterialThemeSurface(): Color =
    androidx.compose.material3.MaterialTheme.colorScheme.surface
