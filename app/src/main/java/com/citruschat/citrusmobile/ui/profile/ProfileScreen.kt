package com.citruschat.citrusmobile.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citruschat.citrusmobile.ui.profile.component.ProfileHeader
import com.citruschat.citrusmobile.ui.profile.component.ProfileOptions

@Composable
fun ProfileScreen(
    onLogoutComplete: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) onLogoutComplete()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        ProfileHeader(user = uiState.user)

        Spacer(modifier = Modifier.weight(1f))

        ProfileOptions(
            isDarkTheme = uiState.isDarkTheme,
            isLoggingOut = uiState.isLoggingOut,
            onDarkThemeChange = viewModel::setDarkTheme,
            onLogoutClick = viewModel::logout,
        )
    }
}
