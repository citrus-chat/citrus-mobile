package com.citruschat.citrusmobile.data.message

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.auth.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageApiClient
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val tokenStore: TokenStore,
        private val logger: Logger,
        baseUrl: String,
    ) : MessageRemoteDataSource {
        private val apiBaseUrl = "${baseUrl.trimEnd('/')}/api/v1"
        private val sendMessageUrl = "$apiBaseUrl/messages/send"

        override suspend fun sendMessage(request: SendEncryptedMessageRequest) {
            withContext(Dispatchers.IO) {
                val payload =
                    JSONObject()
                        .put("messageId", request.messageId)
                        .put("chatRoomId", request.chatRoomId)
                        .put("senderDeviceId", request.senderDeviceId)
                        .put("keyVersion", request.keyVersion)
                        .put("iv", request.iv)
                        .put("ciphertext", request.ciphertext)
                        .apply {
                            request.replyMessageId?.let { put("replyMessageId", it) }
                        }.toString()
                        .toRequestBody(JSON_MEDIA_TYPE)

                okHttpClient.newCall(authorizedRequest(sendMessageUrl).post(payload).build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        logger.w(TAG, "Send message failed code=${response.code}")
                        throw IllegalStateException("Send message failed with HTTP ${response.code}")
                    }
                }
            }
        }

        override suspend fun syncMessages(
            chatRoomId: String,
            lastCreatedAt: String?,
        ): List<RemoteEncryptedMessage> =
            withContext(Dispatchers.IO) {
                val url =
                    "$apiBaseUrl/chatroom/$chatRoomId/sync/messages"
                        .toHttpUrl()
                        .newBuilder()
                        .apply { lastCreatedAt?.let { addQueryParameter("lastCreatedAt", it) } }
                        .build()

                okHttpClient.newCall(authorizedRequest(url.toString()).get().build()).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        logger.w(TAG, "Sync messages failed code=${response.code}")
                        return@withContext emptyList()
                    }

MessageApiResponseParser.parseSyncMessages(body)
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
            const val TAG = "MessageApiClient"
        }
    }
