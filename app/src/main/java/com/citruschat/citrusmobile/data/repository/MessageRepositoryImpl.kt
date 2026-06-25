package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.crypto.ChatCryptoService
import com.citruschat.citrusmobile.data.crypto.EncryptedPayload
import com.citruschat.citrusmobile.data.device.DeviceIdentityProvider
import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.local.dao.ConversationKeyDao
import com.citruschat.citrusmobile.data.local.dao.MessageDao
import com.citruschat.citrusmobile.data.local.dao.UserDao
import com.citruschat.citrusmobile.data.local.entity.MessageEntity
import com.citruschat.citrusmobile.data.mapper.toDomain
import com.citruschat.citrusmobile.data.mapper.toEntity
import com.citruschat.citrusmobile.data.message.MessageRemoteDataSource
import com.citruschat.citrusmobile.data.message.RemoteEncryptedMessage
import com.citruschat.citrusmobile.data.message.SendEncryptedMessageRequest
import com.citruschat.citrusmobile.domain.model.Message
import com.citruschat.citrusmobile.domain.model.MessageDeliveryStatus
import com.citruschat.citrusmobile.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class MessageRepositoryImpl
    @Inject
    constructor(
        private val dao: MessageDao,
        private val chatDao: ChatDao,
        private val userDao: UserDao,
        private val conversationKeyDao: ConversationKeyDao,
        private val remoteDataSource: MessageRemoteDataSource,
        private val deviceIdentityProvider: DeviceIdentityProvider,
        private val cryptoService: ChatCryptoService,
        private val logger: Logger,
    ) : MessageRepository {
        override fun observeMessages(chatId: Long): Flow<List<Message>> =
            dao
                .observeByChatId(chatId)
                .map { entities ->
                    logger.v(TAG, "Observed messages update for chatId=$chatId count=${entities.size}")
                    entities.map { it.toDomain() }
                }

        override suspend fun syncMessages(chatId: Long) {
            val chat = chatDao.getById(chatId) ?: return
            val remoteChatId = chat.remoteId ?: return
            val lastCreatedAt = dao.maxTimestampByChatId(chatId)?.let { Instant.ofEpochMilli(it).toString() }
            val currentUser = userDao.observeCurrentUser().firstOrNull()
            val identity = deviceIdentityProvider.getOrCreateDeviceIdentity()

            remoteDataSource.syncMessages(remoteChatId, lastCreatedAt).forEach { remoteMessage ->
                val key =
                    conversationKeyDao.getConversationKey(remoteChatId, remoteMessage.keyVersion)
                        ?: run {
                            logger.w(TAG, "Skipping message without conversation key messageId=${remoteMessage.id}")
                            return@forEach
                        }
                val content =
                    runCatching {
                        cryptoService.decrypt(
                            payload = EncryptedPayload(remoteMessage.iv, remoteMessage.ciphertext),
                            key = key.key,
                        )
                    }.onFailure { throwable ->
                        logger.e(TAG, "Message decrypt failed messageId=${remoteMessage.id}", throwable)
                    }.getOrNull() ?: return@forEach

                val timestamp = remoteMessage.createdAt.toEpochMillisOrNow()
                val isOwn = remoteMessage.senderDeviceId == identity.deviceId || remoteMessage.senderUserId == currentUser?.id
                val entity =
                    remoteMessage.toEntity(
                        chatId = chatId,
                        text = content,
                        timestamp = timestamp,
                        isOwn = isOwn,
                    )
                val insertedId = dao.insertIgnoringConflict(entity)
                if (insertedId > 0) {
                    dao.updateChatLastMessage(chatId = chatId, messageId = insertedId)
                }
            }
        }

        override suspend fun sendMessage(message: Message) {
            val chat = chatDao.getById(message.chatId) ?: error("Chat not found")
            val remoteChatId = chat.remoteId ?: error("Chat is not synced with the server")
            val conversationKey = conversationKeyDao.getActiveConversationKey(remoteChatId) ?: error("Conversation key not found")
            val identity = deviceIdentityProvider.getOrCreateDeviceIdentity()
            val currentUser = userDao.observeCurrentUser().firstOrNull()
            val senderUserId = currentUser?.id ?: error("Current user not found")
            val encrypted = cryptoService.encrypt(message.text, conversationKey.key)
            val remoteMessageId = UUID.randomUUID().toString()

            val messageToPersist =
                message.copy(
                    remoteId = remoteMessageId,
                    user = currentUser.username.ifBlank { currentUser.email.ifBlank { senderUserId } },
                    isOwn = true,
                    timestamp = message.timestamp.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    deliveryStatus = MessageDeliveryStatus.SENDING,
                    senderUserId = senderUserId,
                    senderDeviceId = identity.deviceId,
                    keyVersion = conversationKey.keyVersion,
                    iv = encrypted.iv,
                    ciphertext = encrypted.ciphertext,
                )

            val localMessageId = dao.insertAndMarkAsLastMessage(messageToPersist.toEntity())
            runCatching {
                remoteDataSource.sendMessage(
                    SendEncryptedMessageRequest(
                        messageId = remoteMessageId,
                        chatRoomId = remoteChatId,
                        senderDeviceId = identity.deviceId,
                        replyMessageId = message.replyToMessageId,
                        keyVersion = conversationKey.keyVersion,
                        iv = encrypted.iv,
                        ciphertext = encrypted.ciphertext,
                    ),
                )
            }.onSuccess {
                dao.updateDeliveryStatus(localMessageId, MessageDeliveryStatus.SENT.name)
                logger.i(TAG, "Message sent chatId=${message.chatId} remoteId=$remoteMessageId")
            }.onFailure { throwable ->
                dao.updateDeliveryStatus(localMessageId, MessageDeliveryStatus.FAILED.name)
                logger.e(TAG, "Message send failed chatId=${message.chatId}", throwable)
                throw throwable
            }
        }

        private fun RemoteEncryptedMessage.toEntity(
            chatId: Long,
            text: String,
            timestamp: Long,
            isOwn: Boolean,
        ): MessageEntity =
            MessageEntity(
                remoteId = id,
                user = senderUserId,
                text = text,
                isOwn = isOwn,
                timestamp = timestamp,
                chatId = chatId,
                deliveryStatus = MessageDeliveryStatus.DELIVERED.name,
                senderUserId = senderUserId,
                senderDeviceId = senderDeviceId,
                replyToMessageId = replyToMessageId,
                keyVersion = keyVersion,
                iv = iv,
                ciphertext = ciphertext,
            )

        private fun String.toEpochMillisOrNow(): Long =
            runCatching { Instant.parse(normalizeIsoFraction()).toEpochMilli() }
                .getOrDefault(System.currentTimeMillis())

        private fun String.normalizeIsoFraction(): String = replace(Regex("""\.(\d{3})\d*Z$"""), ".$1Z")
    }

private const val TAG = "MessageRepository"
