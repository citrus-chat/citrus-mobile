package com.citruschat.citrusmobile.domain.model

data class ChatPermissions(
    val canWrite: Boolean = true,
    val canRead: Boolean = true,
    val canDeleteMessages: Boolean = false,
    val canAddParticipants: Boolean = false,
    val canRemoveParticipants: Boolean = false,
)
