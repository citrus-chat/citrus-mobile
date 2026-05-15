package com.citruschat.citrusmobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        ),
    ],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val user: String,
    val text: String,
    val isOwn: Boolean,
    val timestamp: Long,
    val chatId: Long,
)
