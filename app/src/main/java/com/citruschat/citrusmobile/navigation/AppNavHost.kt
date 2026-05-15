package com.citruschat.citrusmobile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.citruschat.citrusmobile.ui.chat.ChatScreen
import com.citruschat.citrusmobile.ui.home.HomeScreen
import com.citruschat.citrusmobile.ui.login.LoginScreen
import com.citruschat.citrusmobile.ui.splash.SplashAuthGateScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
    ) {
        composable(Routes.Splash) {
            SplashAuthGateScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.Login) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.Home) {
            HomeScreen(
                onChatClick = { chat ->
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
