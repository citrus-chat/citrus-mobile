package com.citruschat.citrusmobile.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ChatListItemEntity(
    @Embedded val chat: ChatEntity,
    @Embedded(prefix = "readState_")
    val readState: ChatReadStateEntity?,
    @Relation(
        parentColumn = "lastMessageId",
        entityColumn = "id",
    )
    val lastMessage: MessageEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy =
            Junction(
                value = ChatParticipantCrossRef::class,
                parentColumn = "chatId",
                entityColumn = "userId",
            ),
    )
    val participants: List<UserEntity> = emptyList(),
)
