package com.citruschat.citrusmobile.ui.home.data

import com.citruschat.citrusmobile.domain.model.MessageDeliveryStatus

data class ChatListItem(
    val id: Long,
    val name: String,
    val lastMessagePreview: String?,
    val participantUserIds: List<String> = emptyList(),
    val participantUsernames: List<String> = emptyList(),
    val participantAvatarUrls: List<String?> = emptyList(),
    val lastMessageStatus: MessageDeliveryStatus? = null,
    val activityText: String? = null,
)
