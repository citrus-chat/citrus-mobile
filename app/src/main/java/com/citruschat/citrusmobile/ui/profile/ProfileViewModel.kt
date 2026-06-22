package com.citruschat.citrusmobile.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.domain.model.User
import com.citruschat.citrusmobile.domain.repository.AuthRepository
import com.citruschat.citrusmobile.domain.repository.ThemeRepository
import com.citruschat.citrusmobile.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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
        private val userRepository: UserRepository,
        private val logger: Logger,
    ) : ViewModel() {
        private val isLoggingOut = MutableStateFlow(false)
        private val isLoggedOut = MutableStateFlow(false)
        private val avatarLocalPath = MutableStateFlow<String?>(null)
        private val isAvatarUploading = MutableStateFlow(false)
        private val currentUser =
            userRepository
                .observeCurrentUser()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = null,
                )

        init {
            viewModelScope.launch {
                runCatching { userRepository.refreshCurrentUser() }
                    .onFailure { throwable -> logger.e(TAG, "Current user refresh failed", throwable) }
            }
            viewModelScope.launch {
                currentUser.collectLatest { user ->
                    avatarLocalPath.value = user?.loadAvatarLocalPath()
                }
            }
        }

        private val profileBaseState =
            combine(
                currentUser,
                avatarLocalPath,
                isAvatarUploading,
                themeRepository.observeDarkTheme(),
            ) { user, avatarPath, avatarUploading, isDarkTheme ->
                ProfileBaseState(
                    user = user,
                    avatarLocalPath = avatarPath,
                    isAvatarUploading = avatarUploading,
                    isDarkTheme = isDarkTheme,
                )
            }

        val uiState: StateFlow<ProfileUiState> =
            combine(profileBaseState, isLoggingOut, isLoggedOut) { baseState, loggingOut, loggedOut ->
                ProfileUiState(
                    user = baseState.user,
                    avatarLocalPath = baseState.avatarLocalPath,
                    isAvatarUploading = baseState.isAvatarUploading,
                    isDarkTheme = baseState.isDarkTheme,
                    isLoggingOut = loggingOut,
                    isLoggedOut = loggedOut,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ProfileUiState(),
            )

        fun uploadAvatar(
            bytes: ByteArray,
            fileName: String,
            mimeType: String,
        ) {
            if (isAvatarUploading.value) return

            viewModelScope.launch {
                isAvatarUploading.value = true
                runCatching {
                    userRepository.uploadCurrentUserAvatar(bytes, fileName, mimeType)
                }.onSuccess { user ->
                    avatarLocalPath.value = user?.loadAvatarLocalPath()
                }.onFailure { throwable ->
                    logger.e(TAG, "Avatar upload failed", throwable)
                }
                isAvatarUploading.value = false
            }
        }

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

        private suspend fun User.loadAvatarLocalPath(): String? = userRepository.getAvatarLocalPath(this)
    }

private data class ProfileBaseState(
    val user: User?,
    val avatarLocalPath: String?,
    val isAvatarUploading: Boolean,
    val isDarkTheme: Boolean,
)

private const val TAG = "ProfileViewModel"
