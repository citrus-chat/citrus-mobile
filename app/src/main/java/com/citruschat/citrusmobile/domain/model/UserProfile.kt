package com.citruschat.citrusmobile.domain.model

data class UserProfile(
    val userId: String,
    val username: String,
    val email: String = "",
    val avatarUrl: String? = null,
    val description: String = "",
    val privacy: String = DEFAULT_PRIVACY,
    val showPhone: Boolean = false,
    val showEmail: Boolean = false,
    val showStatus: Boolean = false,
    val showDescription: Boolean = false,
    val allowGroupInvites: Boolean = false,
) {
    companion object {
        const val DEFAULT_PRIVACY = "private"
        const val MAX_DESCRIPTION_LENGTH = 500
    }
}
