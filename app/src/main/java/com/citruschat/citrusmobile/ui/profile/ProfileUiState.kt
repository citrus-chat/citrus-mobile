package com.citruschat.citrusmobile.ui.profile

import com.citruschat.citrusmobile.domain.model.UserProfile

data class ProfileUiState(
    val profile: UserProfile? = null,
    val avatarLocalPath: String? = null,
    val description: String = "",
    val isAvatarUploading: Boolean = false,
    val isProfileSaving: Boolean = false,
    val isDarkTheme: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isLoggedOut: Boolean = false,
)
