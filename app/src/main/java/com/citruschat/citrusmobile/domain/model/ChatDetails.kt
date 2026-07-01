package com.citruschat.citrusmobile.domain.model

import com.citruschat.citrusmobile.data.local.entity.type.ChatType

data class ChatDetails(
    val id: Long,
    val remoteId: String? = null,
    val name: String,
    val type: ChatType,
    val localProfilePicturePath: String? = null,
    val remoteProfilePictureUrl: String? = null,
    val participants: List<ChatParticipant> = emptyList(),
    val firstUnreadMessageId: Long? = null,
) {
    val isGroup: Boolean = type == ChatType.GROUP
}
