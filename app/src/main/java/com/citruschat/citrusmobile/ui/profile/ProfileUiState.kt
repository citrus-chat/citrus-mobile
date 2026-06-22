package com.citruschat.citrusmobile.ui.profile

import com.citruschat.citrusmobile.domain.model.User

data class ProfileUiState(
    val user: User? = null,
    val avatarLocalPath: String? = null,
    val isAvatarUploading: Boolean = false,
    val isDarkTheme: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isLoggedOut: Boolean = false,
)
