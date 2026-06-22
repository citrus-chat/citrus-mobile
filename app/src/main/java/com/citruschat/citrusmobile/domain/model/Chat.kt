package com.citruschat.citrusmobile.domain.model

import com.citruschat.citrusmobile.data.local.entity.type.ChatType

data class Chat(
    val id: Long = 0,
    val name: String = "",
    val type: ChatType = ChatType.DIRECT,
    val lastMessageId: Long? = null,
    val participantUserIds: List<String> = emptyList(),
    val remoteProfilePictureUrl: String? = null,
    val localProfilePicturePath: String? = null,
)
