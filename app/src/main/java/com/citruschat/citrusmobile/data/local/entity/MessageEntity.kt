package com.citruschat.citrusmobile.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.citruschat.citrusmobile.domain.model.MessageDeliveryStatus

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("chatId"),
        Index(value = ["remoteId"], unique = true),
    ],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,
    val user: String,
    val text: String,
    val isOwn: Boolean,
    val timestamp: Long,
    val chatId: Long,
    val deliveryStatus: String = MessageDeliveryStatus.SENT.toString(),
    val senderUserId: String? = null,
    val senderDeviceId: String? = null,
    val replyToMessageId: String? = null,
    val keyVersion: Int? = null,
    val iv: String? = null,
    val ciphertext: String? = null,
)
