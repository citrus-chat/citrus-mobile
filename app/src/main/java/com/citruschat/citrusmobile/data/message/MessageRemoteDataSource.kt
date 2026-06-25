package com.citruschat.citrusmobile.data.message

interface MessageRemoteDataSource {
    suspend fun sendMessage(request: SendEncryptedMessageRequest)

    suspend fun syncMessages(
        chatRoomId: String,
        lastCreatedAt: String?,
    ): List<RemoteEncryptedMessage>
}

data class SendEncryptedMessageRequest(
    val messageId: String,
    val chatRoomId: String,
    val senderDeviceId: String,
    val replyMessageId: String? = null,
    val keyVersion: Int,
    val iv: String,
    val ciphertext: String,
)

data class RemoteEncryptedMessage(
    val id: String,
    val chatRoomId: String,
    val senderUserId: String,
    val senderDeviceId: String,
    val replyToMessageId: String?,
    val keyVersion: Int,
    val iv: String,
    val ciphertext: String,
    val createdAt: String,
    val editedAt: String?,
    val deletedAt: String?,
)
