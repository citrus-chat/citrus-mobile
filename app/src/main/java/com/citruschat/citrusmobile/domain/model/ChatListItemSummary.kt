package com.citruschat.citrusmobile.domain.model

import com.citruschat.citrusmobile.data.local.entity.type.ChatType

data class ChatListItemSummary(
    val id: Long,
    val name: String,
    val type: ChatType = ChatType.DIRECT,
    val lastMessagePreview: String?,
    val localProfilePicturePath: String? = null,
    val remoteProfilePictureUrl: String? = null,
    val participantUserIds: List<String> = emptyList(),
    val participantUsernames: List<String> = emptyList(),
    val participantAvatarUrls: List<String?> = emptyList(),
    val lastMessageStatus: MessageDeliveryStatus? = null,
    val activityText: String? = null,
)
