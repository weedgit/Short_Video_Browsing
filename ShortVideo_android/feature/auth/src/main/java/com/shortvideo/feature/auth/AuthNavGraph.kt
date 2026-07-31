package com.shortvideo.feature.auth

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shortvideo.core.DestinationRoute

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    onAuthCompleted: (returnRoute: String?) -> Unit,
) {
    composable(
        route = "${DestinationRoute.AUTH_LOGIN_ROUTE}?${DestinationRoute.AUTH_RETURN_ROUTE_ARG}={${DestinationRoute.AUTH_RETURN_ROUTE_ARG}}",
        arguments = listOf(
            navArgument(DestinationRoute.AUTH_RETURN_ROUTE_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) { backStackEntry ->
        val returnRoute = backStackEntry.arguments?.getString(DestinationRoute.AUTH_RETURN_ROUTE_ARG)
        LoginScreen(
            onLoginSuccess = { onAuthCompleted(returnRoute) },
            onNavigateToRegister = {
                navController.navigate(DestinationRoute.authRegisterRoute(returnRoute))
            },
            onNavigateToPasswordReset = {
                navController.navigate(DestinationRoute.AUTH_PASSWORD_RESET_ROUTE)
            },
        )
    }

    composable(
        route = "${DestinationRoute.AUTH_REGISTER_ROUTE}?${DestinationRoute.AUTH_RETURN_ROUTE_ARG}={${DestinationRoute.AUTH_RETURN_ROUTE_ARG}}",
        arguments = listOf(
            navArgument(DestinationRoute.AUTH_RETURN_ROUTE_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) { backStackEntry ->
        val returnRoute = backStackEntry.arguments?.getString(DestinationRoute.AUTH_RETURN_ROUTE_ARG)
        RegisterScreen(
            onRegisterSuccess = { onAuthCompleted(returnRoute) },
            onNavigateToLogin = { navController.popBackStack() },
        )
    }

    composable(DestinationRoute.AUTH_PASSWORD_RESET_ROUTE) {
        PasswordResetScreen(
            onCompleted = {
                navController.navigate(DestinationRoute.AUTH_LOGIN_ROUTE) {
                    popUpTo(DestinationRoute.AUTH_PASSWORD_RESET_ROUTE) {
                        inclusive = true
                    }
                }
            },
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
