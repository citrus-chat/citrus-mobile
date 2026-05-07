package com.citruschat.citrusmobile.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

const val SPLASH_DELAY_MS = 2000L

@Composable
fun SplashAuthGateScreen(
    onNavigateToLogin: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(SPLASH_DELAY_MS) // 2 seconds
        onNavigateToLogin()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
