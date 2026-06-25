package com.citruschat.citrusmobile.data.message

import org.json.JSONArray
import org.json.JSONObject

object MessageApiResponseParser {
    fun parseSyncMessages(responseBody: String): List<RemoteEncryptedMessage> =
        JSONObject(responseBody)
            .getJSONObject("data")
            .optJSONArray("messages")
            .orEmptyObjects()
            .map { it.toRemoteEncryptedMessage() }

    private fun JSONObject.toRemoteEncryptedMessage(): RemoteEncryptedMessage =
        RemoteEncryptedMessage(
            id = getString("id"),
            chatRoomId = getString("chatRoomId"),
            senderUserId = getString("senderUserId"),
            senderDeviceId = getString("senderDeviceId"),
            replyToMessageId = optStringOrNull("replyToMessageId"),
            keyVersion = getInt("keyVersion"),
            iv = getString("iv"),
            ciphertext = getString("ciphertext"),
            createdAt = getString("createdAt"),
            editedAt = optStringOrNull("editedAt"),
            deletedAt = optStringOrNull("deletedAt"),
        )

    private fun JSONArray?.orEmptyObjects(): List<JSONObject> {
        if (this == null) return emptyList()
        return List(length()) { index -> getJSONObject(index) }
    }

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }
}
