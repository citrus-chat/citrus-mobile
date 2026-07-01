package com.citruschat.citrusmobile.data.chat

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.data.local.entity.type.ChatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatApiClient
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val tokenStore: TokenStore,
        private val logger: Logger,
        baseUrl: String,
    ) {
        private val apiBaseUrl = baseUrl.trimEnd('/')
        private val createUrl = "$apiBaseUrl/api/v1/chatroom/create"
        private val syncUrl = "$apiBaseUrl/api/v1/chatroom/sync"
        private val conversationKeysUrl = "$apiBaseUrl/api/v1/chatroom/conversation-keys"

        suspend fun createChatRoom(
            type: ChatType,
            name: String,
            participantIds: List<String>,
        ): String? =
            withContext(Dispatchers.IO) {
                val payload =
                    JSONObject()
                        .put("chatRoomType", type.name)
                        .put("name", name.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                        .put("participantIds", JSONArray(participantIds))
                        .toString()
                        .toRequestBody(JSON_MEDIA_TYPE)

                val request = authorizedRequest(createUrl).post(payload).build()
                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            logger.w(TAG, "Create chatroom failed code=${response.code} body=$body")
                            return@withContext null
                        }
                        val root = JSONObject(body)
                        val data = root.optJSONObject("data") ?: root
                        data.optString("id").takeIf { it.isNotBlank() }
                    }
                } catch (e: IOException) {
                    logger.e(TAG, "Create chatroom network failure", e)
                    null
                }
            }

        suspend fun syncChatRooms(deviceId: String): ChatRoomSyncResult =
            withContext(Dispatchers.IO) {
                val request = authorizedRequest("$syncUrl?deviceId=$deviceId").get().build()
                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            logger.w(TAG, "Sync chatrooms failed code=${response.code} body=$body")
                            return@withContext ChatRoomSyncResult()
                        }
                        parseSyncResponse(body)
                    }
                } catch (e: IOException) {
                    logger.e(TAG, "Sync chatrooms network failure", e)
                    ChatRoomSyncResult()
                }
            }

        suspend fun getUserDevices(userId: String): List<DevicePublicKey> =
            withContext(Dispatchers.IO) {
                val request = authorizedRequest("$apiBaseUrl/api/v1/users/$userId/keys").get().build()
                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            logger.w(TAG, "Get user devices failed code=${response.code} body=$body")
                            return@withContext emptyList()
                        }
                        val data = JSONObject(body).optJSONObject("data") ?: return@withContext emptyList()
                        val devices = data.optJSONArray("devices") ?: return@withContext emptyList()
                        buildList {
                            for (index in 0 until devices.length()) {
                                parseDevicePublicKey(devices.optJSONObject(index))?.let(::add)
                            }
                        }
                    }
                } catch (e: IOException) {
                    logger.e(TAG, "Get user devices network failure", e)
                    emptyList()
                }
            }

        suspend fun getDevicePublicKey(deviceId: String): DevicePublicKey? =
            withContext(Dispatchers.IO) {
                val request = authorizedRequest("$apiBaseUrl/api/v1/devices/$deviceId/keys").get().build()
                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            logger.w(TAG, "Get device public key failed code=${response.code} body=$body")
                            return@withContext null
                        }
                        val data = JSONObject(body).optJSONObject("data") ?: JSONObject(body)
                        parseDevicePublicKey(data)
                    }
                } catch (e: IOException) {
                    logger.e(TAG, "Get device public key network failure", e)
                    null
                }
            }

        suspend fun uploadConversationKey(request: UploadConversationKey): Boolean =
            withContext(Dispatchers.IO) {
                val payload =
                    JSONObject()
                        .put("conversationId", request.conversationId)
                        .put("targetUserId", request.targetUserId)
                        .put("targetDeviceId", request.targetDeviceId)
                        .put("senderDeviceId", request.senderDeviceId)
                        .put("keyVersion", request.keyVersion)
                        .put("ciphertext", request.ciphertext)
                        .put("iv", request.iv)
                        .toString()
                        .toRequestBody(JSON_MEDIA_TYPE)
                val httpRequest = authorizedRequest(conversationKeysUrl).post(payload).build()
                try {
                    okHttpClient.newCall(httpRequest).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            logger.w(TAG, "Upload conversation key failed code=${response.code} body=$body")
                            return@withContext false
                        }
                        true
                    }
                } catch (e: IOException) {
                    logger.e(TAG, "Upload conversation key network failure", e)
                    false
                }
            }

        private fun parseSyncResponse(body: String): ChatRoomSyncResult {
            val root = JSONObject(body)
            val data = root.optJSONObject("data") ?: root
            val chatRooms = data.optJSONArray("chatRooms") ?: JSONArray()
            val conversationKeys = data.optJSONArray("conversationKeys") ?: JSONArray()
            return ChatRoomSyncResult(
                chatRooms = chatRooms.toSyncedChatRooms(),
                conversationKeys = conversationKeys.toSyncedConversationKeys(),
            )
        }

        private fun JSONArray.toSyncedChatRooms(): List<SyncedChatRoom> =
            buildList {
                for (index in 0 until length()) {
                    parseSyncedChatRoom(optJSONObject(index))?.let(::add)
                }
            }

        private fun parseSyncedChatRoom(item: JSONObject?): SyncedChatRoom? {
            item ?: return null
            val id = item.optString("id").takeIf { it.isNotBlank() } ?: return null
            val participants = item.optJSONArray("participants") ?: JSONArray()
            return SyncedChatRoom(
                id = id,
                type = item.optString("type"),
                name = item.optString("name"),
                participantUserIds = participants.toParticipantUserIds(),
            )
        }

        private fun JSONArray.toParticipantUserIds(): List<String> =
            buildList {
                for (index in 0 until length()) {
                    optJSONObject(index)
                        ?.optString("userId")
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::add)
                }
            }.distinct()

        private fun JSONArray.toSyncedConversationKeys(): List<SyncedConversationKey> =
            buildList {
                for (index in 0 until length()) {
                    parseSyncedConversationKey(optJSONObject(index))?.let(::add)
                }
            }

        private fun parseSyncedConversationKey(item: JSONObject?): SyncedConversationKey? {
            item ?: return null
            val conversationId = item.optString("conversationId").takeIf { it.isNotBlank() } ?: return null
            val senderDeviceId = item.optString("senderDeviceId").takeIf { it.isNotBlank() } ?: return null
            val ciphertext = item.optString("ciphertext").takeIf { it.isNotBlank() } ?: return null
            val iv = item.optString("iv").takeIf { it.isNotBlank() } ?: return null
            return SyncedConversationKey(
                conversationId = conversationId,
                senderDeviceId = senderDeviceId,
                keyVersion = item.optInt("keyVersion", NO_KEY_VERSION),
                ciphertext = ciphertext,
                iv = iv,
            )
        }

        private fun parseDevicePublicKey(item: JSONObject?): DevicePublicKey? {
            item ?: return null
            val deviceId = item.optString("deviceId").ifBlank { item.optString("id") }
            val publicKey = item.optString("publicKey")
            if (deviceId.isBlank() || publicKey.isBlank()) return null
            return DevicePublicKey(deviceId = deviceId, publicKey = publicKey)
        }

        private suspend fun authorizedRequest(url: String): Request.Builder {
            val builder = Request.Builder().url(url)
            tokenStore.observeTokens().firstOrNull()?.accessToken?.takeIf { it.isNotBlank() }?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }
            return builder
        }

        private companion object {
            val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
            const val NO_KEY_VERSION = -1
            const val TAG = "ChatApiClient"
        }
    }

data class ChatRoomSyncResult(
    val chatRooms: List<SyncedChatRoom> = emptyList(),
    val conversationKeys: List<SyncedConversationKey> = emptyList(),
)

data class SyncedChatRoom(
    val id: String,
    val type: String,
    val name: String,
    val participantUserIds: List<String>,
)

data class SyncedConversationKey(
    val conversationId: String,
    val senderDeviceId: String,
    val keyVersion: Int,
    val ciphertext: String,
    val iv: String,
)

data class DevicePublicKey(
    val deviceId: String,
    val publicKey: String,
)

data class UploadConversationKey(
    val conversationId: String,
    val targetUserId: String,
    val targetDeviceId: String,
    val senderDeviceId: String,
    val keyVersion: Int,
    val ciphertext: String,
    val iv: String,
)
