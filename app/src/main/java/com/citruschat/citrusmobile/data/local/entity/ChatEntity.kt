package com.citruschat.citrusmobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.citruschat.citrusmobile.data.local.entity.type.ChatType

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
    val type: ChatType = ChatType.DIRECT,
    // For GROUP: real group name
    // For DIRECT: optional cached/display fallback, or empty
    val name: String,
    val lastMessageId: Long? = null,
    // Only for GROUP/custom chat avatar
    val remoteProfilePictureUrl: String? = null,
    val localProfilePicturePath: String? = null,
)
