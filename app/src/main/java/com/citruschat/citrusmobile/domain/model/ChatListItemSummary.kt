package com.citruschat.citrusmobile.domain.model

data class ChatListItemSummary(
    val id: Long,
    val name: String,
    val lastMessagePreview: String?,
    val participantUserIds: List<String> = emptyList(),
    val participantUsernames: List<String> = emptyList(),
    val participantAvatarUrls: List<String?> = emptyList(),
    val lastMessageStatus: MessageDeliveryStatus? = null,
    val activityText: String? = null,
)
