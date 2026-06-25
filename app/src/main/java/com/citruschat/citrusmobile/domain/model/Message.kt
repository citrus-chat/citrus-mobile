package com.citruschat.citrusmobile.domain.model

data class Message(
    val id: Long = 0,
    val remoteId: String? = null,
    val user: String,
    val text: String,
    val timestamp: Long,
    val isOwn: Boolean = false,
    val chatId: Long = 0,
    val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
    val senderUserId: String? = null,
    val senderDeviceId: String? = null,
    val replyToMessageId: String? = null,
    val keyVersion: Int? = null,
    val iv: String? = null,
    val ciphertext: String? = null,
)
