package com.citruschat.citrusmobile.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.domain.repository.AuthRepository
import com.citruschat.citrusmobile.domain.repository.ThemeRepository
import com.citruschat.citrusmobile.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val themeRepository: ThemeRepository,
        userRepository: UserRepository,
        private val logger: Logger,
    ) : ViewModel() {
        private val isLoggingOut = MutableStateFlow(false)
        private val isLoggedOut = MutableStateFlow(false)

        val uiState: StateFlow<ProfileUiState> =
            combine(
                userRepository.observeCurrentUser(),
                themeRepository.observeDarkTheme(),
                isLoggingOut,
                isLoggedOut,
            ) { user, isDarkTheme, loggingOut, loggedOut ->
                ProfileUiState(
                    user = user,
                    isDarkTheme = isDarkTheme,
                    isLoggingOut = loggingOut,
                    isLoggedOut = loggedOut,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ProfileUiState(),
            )

        fun setDarkTheme(enabled: Boolean) {
            viewModelScope.launch {
                logger.d(TAG, "Theme changed dark=$enabled")
                themeRepository.setDarkTheme(enabled)
            }
        }

        fun logout() {
            if (isLoggingOut.value) return

            viewModelScope.launch {
                logger.i(TAG, "Logout requested from profile")
                isLoggingOut.value = true
                runCatching { authRepository.logout() }
                    .onFailure { throwable -> logger.e(TAG, "Logout failed", throwable) }
                isLoggingOut.value = false
                isLoggedOut.value = true
            }
        }
    }

private const val TAG = "ProfileViewModel"
