package com.citruschat.citrusmobile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.citruschat.citrusmobile.ui.chat.ChatScreen
import com.citruschat.citrusmobile.ui.login.LoginScreen
import com.citruschat.citrusmobile.ui.splash.SplashAuthGateScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash",
    ) {
        composable("splash") {
            SplashAuthGateScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
            )
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
            )
        }

        composable("home") {
            ChatScreen()
        }
    }
}
