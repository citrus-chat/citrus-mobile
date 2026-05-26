package com.citruschat.citrusmobile.domain.model

data class ChatListItemSummary(
    val id: Long,
    val name: String,
    val lastMessagePreview: String?,
)
