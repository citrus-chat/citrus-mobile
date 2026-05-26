package com.citruschat.citrusmobile.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ChatListItemEntity(
    @Embedded val chat: ChatEntity,
    @Relation(
        parentColumn = "lastMessageId",
        entityColumn = "id",
    )
    val lastMessage: MessageEntity?,
)
