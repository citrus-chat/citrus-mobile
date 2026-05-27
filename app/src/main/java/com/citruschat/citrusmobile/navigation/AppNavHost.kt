package com.citruschat.citrusmobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.ui.chat.ChatScreen
import com.citruschat.citrusmobile.ui.home.HomeScreen
import com.citruschat.citrusmobile.ui.login.LoginScreen
import com.citruschat.citrusmobile.ui.splash.SplashAuthGateScreen

private const val TAG = "AppNavHost"

@Composable
fun AppNavHost(logger: Logger) {
    val navController = rememberNavController()
    LaunchedEffect(Unit) {
        logger.i(TAG, "Navigation graph initialized with start=${Routes.Splash}")
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
    ) {
        composable(Routes.Splash) {
            SplashAuthGateScreen(
                onNavigateToHome = {
                    logger.i(TAG, "Navigate: splash -> home")
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    logger.i(TAG, "Navigate: splash -> login")
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.Login) {
            LoginScreen(
                onLoginSuccess = {
                    logger.i(TAG, "Navigate: login -> home")
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.Home) {
            HomeScreen(
                onChatClick = { chat ->
                    logger.i(TAG, "Navigate: home -> chat/${chat.id}")
                    navController.navigate(Routes.chat(chat.id))
                },
            )
        }

        composable(
            route = Routes.Chat,
            arguments = listOf(navArgument("chatId") { type = NavType.LongType }),
        ) {
            ChatScreen()
        }
    }
}
