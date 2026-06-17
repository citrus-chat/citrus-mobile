package com.citruschat.citrusmobile.domain.model

data class User(
    val id: String,
    val email: String,
    val username: String,
    val profilePictureUrl: String? = null,
    val statusMessage: String? = null,
    val createdAt: String = "",
    val isCurrentUser: Boolean = false,
)
