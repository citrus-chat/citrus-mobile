package com.citruschat.citrusmobile.domain.model

data class ChatParticipant(
    val id: String,
    val email: String,
    val username: String,
    val remoteProfilePictureUrl: String? = null,
    val localProfilePicturePath: String? = null,
    val statusMessage: String? = null,
    val createdAt: String = "",
    val isCurrentUser: Boolean = false,
)
