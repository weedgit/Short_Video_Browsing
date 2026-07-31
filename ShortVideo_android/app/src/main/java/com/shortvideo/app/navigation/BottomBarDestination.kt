package com.shortvideo.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.shortvideo.core.DestinationRoute
import com.shortvideo.theme.R

enum class BottomBarDestination(
    val route: String,
    @StringRes val titleRes: Int?,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val emphasized: Boolean = false,
) {
    HOME(
        route = DestinationRoute.HOME_ROUTE,
        titleRes = R.string.tab_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    DISCOVER(
        route = DestinationRoute.DISCOVER_ROUTE,
        titleRes = R.string.tab_discover,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
    ),
    UPLOAD(
        route = DestinationRoute.UPLOAD_ROUTE,
        titleRes = R.string.tab_upload,
        selectedIcon = Icons.Filled.Add,
        unselectedIcon = Icons.Filled.Add,
        emphasized = true,
    ),
    INBOX(
        route = DestinationRoute.INBOX_ROUTE,
        titleRes = R.string.tab_inbox,
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
    ),
    PROFILE(
        route = DestinationRoute.PROFILE_ROUTE,
        titleRes = R.string.tab_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
    ),
}
