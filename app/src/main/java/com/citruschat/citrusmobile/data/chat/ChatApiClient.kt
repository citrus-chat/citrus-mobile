package com.citruschat.citrusmobile.data.chat

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.domain.model.ChatPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
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
    ) : ChatRemoteDataSource {
        private val apiBaseUrl = "${baseUrl.trimEnd('/')}/api/v1"

        override suspend fun createChatRoom(request: CreateChatRoomRequest): RemoteChatRoom =
            withContext(Dispatchers.IO) {
                val payload =
                    JSONObject()
                        .put("chatRoomType", request.type.name)
                        .put("participantIds", JSONArray(request.participantIds))
                        .apply { request.name?.takeIf { it.isNotBlank() }?.let { put("name", it) } }
                        .toString()
                        .toRequestBody(JSON_MEDIA_TYPE)

                okHttpClient.newCall(authorizedRequest("$apiBaseUrl/chatroom/create").post(payload).build()).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        logger.w(TAG, "Create chatroom failed code=${response.code}")
                        throw IllegalStateException("Create chatroom failed with HTTP ${response.code}")
                    }
                    ChatApiResponseParser.parseChatRoom(body)
                }
            }

        override suspend fun syncChatRooms(deviceId: String): RemoteChatRoomSync =
            withContext(Dispatchers.IO) {
                val url =
                    "$apiBaseUrl/chatroom/sync"
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("deviceId", deviceId)
                        .build()

                okHttpClient.newCall(authorizedRequest(url.toString()).get().build()).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        logger.w(TAG, "Sync chatrooms failed code=${response.code}")
                        return@withContext RemoteChatRoomSync(emptyList(), emptyList())
                    }
ChatApiResponseParser.parseSyncChatRooms(body)
                }
            }

        override suspend fun uploadConversationKey(request: UploadConversationKeyRequest) {
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

                okHttpClient.newCall(authorizedRequest("$apiBaseUrl/chatroom/conversation-keys").post(payload).build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        logger.w(TAG, "Upload conversation key failed code=${response.code}")
                        throw IllegalStateException("Upload conversation key failed with HTTP ${response.code}")
                    }
                }
            }
        }

        override suspend fun getUserDeviceKeys(userId: String): UserDeviceKeys =
            withContext(Dispatchers.IO) {
                okHttpClient.newCall(authorizedRequest("$apiBaseUrl/users/$userId/keys").get().build()).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        logger.w(TAG, "Get user device keys failed code=${response.code}")
                        throw IllegalStateException("Get user device keys failed with HTTP ${response.code}")
                    }
                    ChatApiResponseParser.parseUserDeviceKeys(body)
                }
            }

        override suspend fun getDeviceKeys(deviceId: String): DevicePublicKey? =
            withContext(Dispatchers.IO) {
                okHttpClient.newCall(authorizedRequest("$apiBaseUrl/devices/$deviceId/keys").get().build()).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        logger.w(TAG, "Get device keys failed code=${response.code}")
                        return@withContext null
                    }
ChatApiResponseParser.parseDevicePublicKey(body)
                }
            }

        override suspend fun getUserPermissions(
            participantId: String,
            chatRoomId: String,
        ): ChatPermissions =
            withContext(Dispatchers.IO) {
                okHttpClient
                    .newCall(authorizedRequest("$apiBaseUrl/chatroom/$chatRoomId/participant/$participantId/permission").get().build())
                    .execute()
                    .use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            logger.w(TAG, "Get chat permissions failed code=${response.code}")
                            return@withContext ChatPermissions()
                        }
                        ChatApiResponseParser.parsePermissions(body)
                    }
            }

        private suspend fun authorizedRequest(url: String): Request.Builder {
            val requestBuilder = Request.Builder().url(url)
            tokenStore.observeTokens().firstOrNull()?.accessToken?.takeIf { it.isNotBlank() }?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }
            return requestBuilder
        }

        private companion object {
            val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
            const val TAG = "ChatApiClient"
        }
    }
