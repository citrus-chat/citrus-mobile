package com.citruschat.citrusmobile.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citruschat.citrusmobile.domain.auth.AuthState
import kotlinx.coroutines.delay

const val SPLASH_DELAY_MS = 2000L

@Composable
fun SplashAuthGateScreen(
    viewModel: SplashAuthGateViewModel = hiltViewModel(),
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var splashDelayDone by rememberSaveable { mutableStateOf(false) }
    var hasNavigated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(SPLASH_DELAY_MS)
        splashDelayDone = true
    }

    LaunchedEffect(authState, splashDelayDone, hasNavigated) {
        if (!splashDelayDone || hasNavigated) return@LaunchedEffect

        when (authState) {
            AuthState.Authenticated -> {
                hasNavigated = true
                onNavigateToMain()
            }
            AuthState.Unauthenticated -> {
                hasNavigated = true
                onNavigateToLogin()
            }
            AuthState.Loading -> Unit
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
