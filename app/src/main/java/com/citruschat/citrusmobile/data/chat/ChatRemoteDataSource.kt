package com.citruschat.citrusmobile.data.chat

import com.citruschat.citrusmobile.data.local.entity.type.ChatType
import com.citruschat.citrusmobile.domain.model.ChatPermissions

interface ChatRemoteDataSource {
    suspend fun createChatRoom(request: CreateChatRoomRequest): RemoteChatRoom

    suspend fun syncChatRooms(deviceId: String): RemoteChatRoomSync

    suspend fun uploadConversationKey(request: UploadConversationKeyRequest)

    suspend fun getUserDeviceKeys(userId: String): UserDeviceKeys

    suspend fun getDeviceKeys(deviceId: String): DevicePublicKey?

    suspend fun getUserPermissions(
        participantId: String,
        chatRoomId: String,
    ): ChatPermissions
}

data class CreateChatRoomRequest(
    val type: ChatType,
    val name: String?,
    val participantIds: List<String>,
)

data class RemoteChatRoomSync(
    val chatRooms: List<RemoteChatRoom>,
    val conversationKeys: List<RemoteConversationKey>,
)

data class RemoteChatRoom(
    val id: String,
    val type: ChatType,
    val name: String,
    val createdBy: String,
    val participants: List<RemoteChatParticipant> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
)

data class RemoteChatParticipant(
    val id: String,
    val chatRoomId: String,
    val userId: String,
)

data class RemoteConversationKey(
    val conversationId: String,
    val senderDeviceId: String,
    val keyVersion: Int,
    val iv: String,
    val ciphertext: String,
    val createdAt: String,
)

data class UploadConversationKeyRequest(
    val conversationId: String,
    val targetUserId: String,
    val targetDeviceId: String,
    val senderDeviceId: String,
    val keyVersion: Int,
    val ciphertext: String,
    val iv: String,
)

data class UserDeviceKeys(
    val userId: String,
    val devices: List<DevicePublicKey>,
)

data class DevicePublicKey(
    val userId: String? = null,
    val deviceId: String,
    val publicKey: String,
)
