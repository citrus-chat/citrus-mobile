package com.citruschat.citrusmobile.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.domain.model.UserProfile
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
        private val userRepository: UserRepository,
        private val logger: Logger,
    ) : ViewModel() {
        private val isLoggingOut = MutableStateFlow(false)
        private val isLoggedOut = MutableStateFlow(false)
        private val avatarLocalPath = MutableStateFlow<String?>(null)
        private val isAvatarUploading = MutableStateFlow(false)
        private val profile = MutableStateFlow<UserProfile?>(null)
        private val description = MutableStateFlow("")
        private val isProfileSaving = MutableStateFlow(false)

        init {
            viewModelScope.launch {
                refreshProfile()
            }
        }

        private val profileBaseState =
            combine(
                avatarLocalPath,
                isAvatarUploading,
                profile,
                description,
                isProfileSaving,
                themeRepository.observeDarkTheme(),
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                ProfileBaseState(
                    avatarLocalPath = values[0] as String?,
                    isAvatarUploading = values[1] as Boolean,
                    profile = values[2] as UserProfile?,
                    description = values[3] as String,
                    isProfileSaving = values[4] as Boolean,
                    isDarkTheme = values[5] as Boolean,
                )
            }

        val uiState: StateFlow<ProfileUiState> =
            combine(profileBaseState, isLoggingOut, isLoggedOut) { baseState, loggingOut, loggedOut ->
                ProfileUiState(
                    profile = baseState.profile,
                    avatarLocalPath = baseState.avatarLocalPath,
                    description = baseState.description,
                    isAvatarUploading = baseState.isAvatarUploading,
                    isProfileSaving = baseState.isProfileSaving,
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
                }.onSuccess {
                    refreshProfile()
                }.onFailure { throwable ->
                    logger.e(TAG, "Avatar upload failed", throwable)
                }
                isAvatarUploading.value = false
            }
        }

        fun setDescription(value: String) {
            description.value = value.take(UserProfile.MAX_DESCRIPTION_LENGTH)
        }

        fun saveDescription() {
            val currentProfile = profile.value ?: return
            updateProfile(currentProfile.copy(description = description.value))
        }

        fun setShowPhone(enabled: Boolean) {
            val currentProfile = profile.value ?: return
            updateProfile(currentProfile.copy(showPhone = enabled))
        }

        fun setShowEmail(enabled: Boolean) {
            val currentProfile = profile.value ?: return
            updateProfile(currentProfile.copy(showEmail = enabled))
        }

        fun setShowStatus(enabled: Boolean) {
            val currentProfile = profile.value ?: return
            updateProfile(currentProfile.copy(showStatus = enabled))
        }

        fun setShowDescription(enabled: Boolean) {
            val currentProfile = profile.value ?: return
            updateProfile(currentProfile.copy(showDescription = enabled))
        }

        fun setAllowGroupInvites(enabled: Boolean) {
            val currentProfile = profile.value ?: return
            updateProfile(currentProfile.copy(allowGroupInvites = enabled))
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

        private fun updateProfile(updatedProfile: UserProfile) {
            if (isProfileSaving.value) return

            viewModelScope.launch {
                isProfileSaving.value = true
                runCatching { userRepository.updateCurrentUserProfile(updatedProfile) }
                    .onSuccess { savedProfile ->
                        if (savedProfile != null) applyProfile(savedProfile)
                    }.onFailure { throwable -> logger.e(TAG, "Current user profile update failed", throwable) }
                isProfileSaving.value = false
            }
        }

        private suspend fun refreshProfile() {
            runCatching { userRepository.getCurrentUserProfile() }
                .onSuccess { loadedProfile ->
                    if (loadedProfile != null) applyProfile(loadedProfile)
                }.onFailure { throwable -> logger.e(TAG, "Current user profile refresh failed", throwable) }
        }

        private suspend fun applyProfile(loadedProfile: UserProfile) {
            profile.value = loadedProfile
            description.value = loadedProfile.description
            avatarLocalPath.value = userRepository.getAvatarLocalPath(loadedProfile.avatarUrl)
        }
    }

private data class ProfileBaseState(
    val avatarLocalPath: String?,
    val isAvatarUploading: Boolean,
    val profile: UserProfile?,
    val description: String,
    val isProfileSaving: Boolean,
    val isDarkTheme: Boolean,
)

private const val TAG = "ProfileViewModel"
