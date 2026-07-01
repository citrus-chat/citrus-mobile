package com.citruschat.citrusmobile.data.message

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.data.crypto.ConversationCrypto
import com.citruschat.citrusmobile.data.crypto.ConversationKeyStore
import com.citruschat.citrusmobile.data.crypto.EncryptedPayload
import com.citruschat.citrusmobile.data.device.DeviceIdentityProvider
import com.citruschat.citrusmobile.domain.model.Message
import com.citruschat.citrusmobile.domain.model.MessageDeliveryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SendMessagePayload(
    val messageId: String,
    val json: String,
)

@Singleton
class MessageApiClient
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val tokenStore: TokenStore,
        private val deviceIdentityProvider: DeviceIdentityProvider,
        private val conversationCrypto: ConversationCrypto,
        private val conversationKeyStore: ConversationKeyStore,
        private val logger: Logger,
        baseUrl: String,
    ) {
        private val apiBaseUrl = baseUrl.trimEnd('/')

        suspend fun createSendPayload(
            chatRemoteId: String,
            text: String,
        ): SendMessagePayload? {
            val device = deviceIdentityProvider.getOrCreateDeviceIdentity()
            val conversationKey = conversationKeyStore.getActiveKey(chatRemoteId)
            if (conversationKey == null) {
                logger.w(TAG, "Cannot create send payload because conversation key is missing chatRoomId=$chatRemoteId")
                return null
            }
            val encrypted = conversationCrypto.encryptMessage(text, conversationKey.key)
            val messageId = UUID.randomUUID().toString()
            val json =
                JSONObject()
                    .put("messageId", messageId)
                    .put("chatRoomId", chatRemoteId)
                    .put("senderDeviceId", device.deviceId)
                    .put("replyMessageId", JSONObject.NULL)
                    .put("keyVersion", conversationKey.keyVersion)
                    .put("iv", encrypted.iv)
                    .put("ciphertext", encrypted.ciphertext)
                    .toString()
            return SendMessagePayload(messageId = messageId, json = json)
        }

        suspend fun sendMessage(
            chatRemoteId: String,
            text: String,
        ): String? =
            withContext(Dispatchers.IO) {
                val payload = createSendPayload(chatRemoteId, text) ?: return@withContext null
                val request =
                    authorizedRequest("$apiBaseUrl/api/v1/messages/send")
                        .post(payload.json.toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            logger.w(TAG, "Send message failed code=${response.code} body=$body")
                            return@withContext null
                        }
                        logger.i(TAG, "Send message accepted by API chatRoomId=$chatRemoteId")
                        payload.messageId
                    }
                } catch (e: IOException) {
                    logger.e(TAG, "Send message network failure", e)
                    null
                }
            }

        suspend fun syncMessages(
            chatId: Long,
            chatRemoteId: String,
            currentUserId: String?,
            lastCreatedAtMillis: Long?,
        ): List<Message> =
            withContext(Dispatchers.IO) {
                val urlBuilder = StringBuilder("$apiBaseUrl/api/v1/chatroom/$chatRemoteId/sync/messages")
                if (lastCreatedAtMillis != null && lastCreatedAtMillis > 0) {
                    urlBuilder.append("?lastCreatedAt=").append(Instant.ofEpochMilli(lastCreatedAtMillis).toString())
                }

                val request = authorizedRequest(urlBuilder.toString()).get().build()

                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            logger.w(TAG, "Sync messages failed code=${response.code} body=$body")
                            return@withContext emptyList()
                        }
                        parseMessages(body, chatId, chatRemoteId, currentUserId)
                    }
                } catch (e: IOException) {
                    logger.e(TAG, "Sync messages network failure", e)
                    emptyList()
                }
            }

        private suspend fun authorizedRequest(url: String): Request.Builder {
            val builder = Request.Builder().url(url)
            tokenStore.observeTokens().firstOrNull()?.accessToken?.takeIf { it.isNotBlank() }?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }
            return builder
        }

        private fun parseMessages(
            body: String,
            chatId: Long,
            chatRemoteId: String,
            currentUserId: String?,
        ): List<Message> {
            val root = JSONObject(body)
            val data = root.optJSONObject("data") ?: root
            val messages = data.optJSONArray("messages") ?: return emptyList()
            return buildList {
                for (index in 0 until messages.length()) {
                    parseMessage(
                        item = messages.optJSONObject(index),
                        chatId = chatId,
                        chatRemoteId = chatRemoteId,
                        currentUserId = currentUserId,
                    )?.let(::add)
                }
            }
        }

        private fun parseMessage(
            item: JSONObject?,
            chatId: Long,
            chatRemoteId: String,
            currentUserId: String?,
        ): Message? {
            item ?: return null
            val senderUserId = item.optString("senderUserId")
            val keyVersion = item.optInt("keyVersion", NO_KEY_VERSION)
            val conversationKey = conversationKeyStore.getKey(chatRemoteId, keyVersion)
            if (conversationKey == null) {
                logger.w(TAG, "Skipping encrypted message because conversation key is missing chatRoomId=$chatRemoteId keyVersion=$keyVersion")
                return null
            }
            val text =
                runCatching {
                    conversationCrypto.decryptMessage(
                        payload =
                            EncryptedPayload(
                                iv = item.optString("iv"),
                                ciphertext = item.optString("ciphertext"),
                            ),
                        conversationKey = conversationKey.key,
                    )
                }.onFailure { throwable ->
                    logger.e(TAG, "Failed to decrypt message id=${item.optString(REMOTE_ID_FIELD)}", throwable)
                }.getOrNull() ?: return null

            return Message(
                id = 0,
                remoteId = item.optString(REMOTE_ID_FIELD).takeIf { it.isNotBlank() },
                user = senderUserId.ifBlank { "Unknown" },
                text = text,
                isOwn = currentUserId != null && senderUserId == currentUserId,
                timestamp = item.optString("createdAt").toEpochMillis(),
                chatId = chatId,
                deliveryStatus = MessageDeliveryStatus.SENT,
            )
        }

        private fun String.toEpochMillis(): Long =
            runCatching { Instant.parse(this).toEpochMilli() }
                .getOrDefault(System.currentTimeMillis())

        private companion object {
            val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
            const val REMOTE_ID_FIELD = "id"
            const val NO_KEY_VERSION = -1
            const val TAG = "MessageApiClient"
        }
    }
