package com.citruschat.citrusmobile.data.chat

import com.citruschat.citrusmobile.data.local.entity.type.ChatType
import com.citruschat.citrusmobile.domain.model.ChatPermissions
import org.json.JSONArray
import org.json.JSONObject

object ChatApiResponseParser {
    fun parseChatRoom(responseBody: String): RemoteChatRoom =
        JSONObject(responseBody)
            .getJSONObject("data")
            .toRemoteChatRoom()

    fun parseSyncChatRooms(responseBody: String): RemoteChatRoomSync {
        val data = JSONObject(responseBody).getJSONObject("data")
        return RemoteChatRoomSync(
            chatRooms = data.optJSONArray("chatRooms").orEmptyObjects().map { it.toRemoteChatRoom() },
            conversationKeys = data.optJSONArray("conversationKeys").orEmptyObjects().map { it.toRemoteConversationKey() },
        )
    }

    fun parseUserDeviceKeys(responseBody: String): UserDeviceKeys =
        JSONObject(responseBody)
            .getJSONObject("data")
            .toUserDeviceKeys()

    fun parseDevicePublicKey(responseBody: String): DevicePublicKey =
        JSONObject(responseBody)
            .getJSONObject("data")
            .let { data ->
                DevicePublicKey(
                    deviceId = data.getString("deviceId"),
                    publicKey = data.getString("publicKey"),
                )
            }

    fun parsePermissions(responseBody: String): ChatPermissions =
        JSONObject(responseBody)
            .getJSONObject("data")
            .toChatPermissions()

    private fun JSONObject.toRemoteChatRoom(): RemoteChatRoom =
        RemoteChatRoom(
            id = getString("id"),
            type = ChatType.valueOf(getString("type")),
            name = optString("name"),
            createdBy = getString("createdBy"),
            participants = optJSONArray("participants").orEmptyObjects().map { it.toRemoteChatParticipant() },
            createdAt = getString("createdAt"),
            updatedAt = getString("updatedAt"),
            deletedAt = optStringOrNull("deletedAt"),
        )

    private fun JSONObject.toRemoteChatParticipant(): RemoteChatParticipant =
        RemoteChatParticipant(
            id = getString("id"),
            chatRoomId = getString("chatRoomId"),
            userId = getString("userId"),
        )

    private fun JSONObject.toRemoteConversationKey(): RemoteConversationKey =
        RemoteConversationKey(
            conversationId = getString("conversationId"),
            senderDeviceId = getString("senderDeviceId"),
            keyVersion = getInt("keyVersion"),
            iv = getString("iv"),
            ciphertext = getString("ciphertext"),
            createdAt = getString("createdAt"),
        )

    private fun JSONObject.toUserDeviceKeys(): UserDeviceKeys {
        val userId = getString("userId")
        return UserDeviceKeys(
            userId = userId,
            devices =
                optJSONArray("devices").orEmptyObjects().map { device ->
                    DevicePublicKey(
                        userId = userId,
                        deviceId = device.getString("deviceId"),
                        publicKey = device.getString("publicKey"),
                    )
                },
        )
    }

    private fun JSONObject.toChatPermissions(): ChatPermissions {
        val codes = optJSONArray("permissions").orEmptyObjects().map { it.getString("code") }.toSet()
        return ChatPermissions(
            canWrite = "CAN_SEND_MESSAGE" in codes,
            canRead = true,
            canDeleteMessages = "CAN_DELETE_MESSAGES" in codes,
            canAddParticipants = "CAN_ADD_PARTICIPANTS" in codes,
            canRemoveParticipants = "CAN_REMOVE_PARTICIPANTS" in codes,
        )
    }

    private fun JSONArray?.orEmptyObjects(): List<JSONObject> {
        if (this == null) return emptyList()
        return List(length()) { index -> getJSONObject(index) }
    }

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }
}
