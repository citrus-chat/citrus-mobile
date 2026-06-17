package com.citruschat.citrusmobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["lastMessageId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("lastMessageId")],
)
data class ChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val lastMessageId: Long? = null,
)
