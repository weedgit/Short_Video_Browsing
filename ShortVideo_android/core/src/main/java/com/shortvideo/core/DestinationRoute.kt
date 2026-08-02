package com.shortvideo.core

import android.net.Uri

object DestinationRoute {
    const val HOME_ROUTE = "home_route"
    const val DISCOVER_ROUTE = "discover_route"
    const val UPLOAD_ROUTE = "upload_route"
    const val INBOX_ROUTE = "inbox_route"
    const val PROFILE_ROUTE = "profile_route"
    const val USER_ID_ARG = "userId"
    const val USER_PROFILE_ROUTE = "user_profile_route/{$USER_ID_ARG}"
    const val AUTH_ROUTE = "auth_route"
    const val AUTH_LOGIN_ROUTE = "auth_login_route"
    const val AUTH_REGISTER_ROUTE = "auth_register_route"
    const val AUTH_PASSWORD_RESET_ROUTE = "auth_password_reset_route"
    const val AUTH_RETURN_ROUTE_ARG = "return_route"
    const val SETTINGS_ROUTE = "settings_route"
    const val ACCESSIBILITY_ONBOARDING_ROUTE = "accessibility_onboarding_route"

    val authRequiredRoutes = setOf(UPLOAD_ROUTE, PROFILE_ROUTE)

    fun userProfileRoute(userId: String): String =
        "user_profile_route/${Uri.encode(userId)}"

    fun authLoginRoute(returnRoute: String? = null): String =
        "$AUTH_LOGIN_ROUTE?$AUTH_RETURN_ROUTE_ARG=${Uri.encode(returnRoute.orEmpty())}"

    fun authRegisterRoute(returnRoute: String? = null): String =
        "$AUTH_REGISTER_ROUTE?$AUTH_RETURN_ROUTE_ARG=${Uri.encode(returnRoute.orEmpty())}"

    fun isAuthRoute(route: String?): Boolean {
        if (route.isNullOrBlank()) return false
        return route == AUTH_LOGIN_ROUTE ||
            route == AUTH_REGISTER_ROUTE ||
            route == AUTH_PASSWORD_RESET_ROUTE ||
            route.startsWith("$AUTH_LOGIN_ROUTE?") ||
            route.startsWith("$AUTH_REGISTER_ROUTE?")
    }
}
