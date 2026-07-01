package com.citruschat.citrusmobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_read_state",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ChatReadStateEntity(
    @PrimaryKey
    val chatId: Long,
    val unreadCount: Int = 0,
    val firstUnreadMessageId: Long? = null,
)
