package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.chat.ChatRemoteDataSource
import com.citruschat.citrusmobile.data.chat.CreateChatRoomRequest
import com.citruschat.citrusmobile.data.chat.DevicePublicKey
import com.citruschat.citrusmobile.data.chat.RemoteChatRoom
import com.citruschat.citrusmobile.data.chat.UploadConversationKeyRequest
import com.citruschat.citrusmobile.data.crypto.ChatCryptoService
import com.citruschat.citrusmobile.data.crypto.EncryptedPayload
import com.citruschat.citrusmobile.data.device.DeviceIdentityProvider
import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.local.dao.ConversationKeyDao
import com.citruschat.citrusmobile.data.local.dao.UserDao
import com.citruschat.citrusmobile.data.local.entity.ChatEntity
import com.citruschat.citrusmobile.data.local.entity.ChatListItemEntity
import com.citruschat.citrusmobile.data.local.entity.ChatParticipantCrossRef
import com.citruschat.citrusmobile.data.local.entity.ConversationKeyEntity
import com.citruschat.citrusmobile.data.local.entity.UserEntity
import com.citruschat.citrusmobile.data.local.entity.type.ChatType
import com.citruschat.citrusmobile.data.mapper.toEntity
import com.citruschat.citrusmobile.data.mapper.toParticipantRefs
import com.citruschat.citrusmobile.data.mapper.toSummary
import com.citruschat.citrusmobile.data.user.UserAvatarLocalDataSource
import com.citruschat.citrusmobile.data.user.UserRemoteDataSource
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.model.ChatPermissions
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import javax.inject.Inject

class ChatRepositoryImpl
    @Inject
    constructor(
        private val dao: ChatDao,
        private val userDao: UserDao,
        private val conversationKeyDao: ConversationKeyDao,
        private val userRemoteDataSource: UserRemoteDataSource,
        private val chatRemoteDataSource: ChatRemoteDataSource,
        private val avatarLocalDataSource: UserAvatarLocalDataSource,
        private val deviceIdentityProvider: DeviceIdentityProvider,
        private val cryptoService: ChatCryptoService,
        private val logger: Logger,
    ) : ChatRepository {
        override fun observeChatsItems(searchQuery: String): Flow<List<ChatListItemSummary>> =
            combine(
                dao.observeItems(searchQuery.trim()),
                userDao.observeCurrentUser(),
            ) { entities, currentUser ->
                logger.v(
                    TAG,
                    "Observed chat list update with count=${entities.size} searchQuery=$searchQuery",
                )
                entities.map { entity ->
                    val repairedEntity = entity.withRepairedDirectParticipant(currentUser?.id)
                    val summary = repairedEntity.toSummary(currentUser?.id)
                    summary.withResolvedAvatar(currentUser?.id)
                }
            }

        override suspend fun findDirectChatId(participantUserIds: List<String>): Long? {
            val distinctParticipantIds = participantUserIds.distinct()
            if (distinctParticipantIds.isEmpty()) return null
            return dao.findDirectChatId(
                participantUserIds = distinctParticipantIds,
                participantCount = distinctParticipantIds.size,
            )
        }

        override suspend fun createChat(chat: Chat): Long {
            logger.i(TAG, "Creating chat name=${chat.name} participants=${chat.participantUserIds.size}")
            val currentUser = userDao.observeCurrentUser().firstOrNull()
            val participantUserIds = chat.participantUserIds.distinct()
            ensureParticipantUsers(participantUserIds)

            val remoteChat =
                if (chat.remoteId == null) {
                    createRemoteChatWithConversationKey(chat, participantUserIds, currentUser?.id)
                } else {
                    null
                }

            val chatToPersist =
                chat.copy(
                    remoteId = remoteChat?.id ?: chat.remoteId,
                    name = remoteChat?.name?.takeIf { it.isNotBlank() } ?: chat.name,
                    type = remoteChat?.type ?: chat.type,
                    createdAt = remoteChat?.createdAt ?: chat.createdAt,
                    updatedAt = remoteChat?.updatedAt ?: chat.updatedAt,
                )

            val chatId =
                runCatching {
                    dao.insert(chatToPersist.toEntity())
                }.onFailure { throwable ->
                    logger.e(TAG, "Create chat failed remoteId=${chatToPersist.remoteId}", throwable)
                    throw throwable
                }.getOrThrow()

            val participantRefs = chatToPersist.toParticipantRefs(chatId = chatId)
            if (participantRefs.isNotEmpty()) {
                dao.insertParticipants(participantRefs)
            }
            logger.d(TAG, "Chat created id=$chatId remoteId=${chatToPersist.remoteId}")
            return chatId
        }

        override suspend fun getRemoteChatId(chatId: Long): String? = dao.getById(chatId)?.remoteId

        override suspend fun syncChats() {
            val identity = deviceIdentityProvider.getOrCreateDeviceIdentity()
            val privateKey = identity.privateKey ?: error("Current device private key not found")
            val remoteSync = chatRemoteDataSource.syncChatRooms(identity.deviceId)

            remoteSync.chatRooms.forEach { remoteChat ->
                upsertRemoteChat(remoteChat)
            }

            remoteSync.conversationKeys.forEach { encryptedKey ->
                val senderDevice = chatRemoteDataSource.getDeviceKeys(encryptedKey.senderDeviceId) ?: return@forEach
                val conversationKey =
                    cryptoService.decryptConversationKeyForUser(
                        payload =
                            EncryptedPayload(
                                iv = encryptedKey.iv,
                                ciphertext = encryptedKey.ciphertext,
                            ),
                        senderPublicKey = senderDevice.publicKey,
                        myPrivateKey = privateKey,
                    )
                conversationKeyDao.upsert(
                    ConversationKeyEntity(
                        conversationId = encryptedKey.conversationId,
                        keyVersion = encryptedKey.keyVersion,
                        key = conversationKey,
                        createdAt = encryptedKey.createdAt,
                    ),
                )
            }
        }

        override suspend fun getPermissions(chatId: Long): ChatPermissions {
            val chat = dao.getById(chatId) ?: return ChatPermissions()
            val remoteChatId = chat.remoteId ?: return ChatPermissions()
            val currentUser = userDao.observeCurrentUser().firstOrNull() ?: return ChatPermissions()
            val participantId = dao.findRemoteParticipantId(chatId, currentUser.id) ?: return ChatPermissions()
            return chatRemoteDataSource.getUserPermissions(participantId, remoteChatId)
        }

        override suspend fun deleteChat(chatId: Long) {
            logger.i(TAG, "Deleting chat id=$chatId")
            runCatching {
                dao.deleteById(chatId)
            }.onFailure { throwable ->
                logger.e(TAG, "Delete chat failed id=$chatId", throwable)
                throw throwable
            }
            logger.d(TAG, "Chat deleted id=$chatId")
        }

        private suspend fun createRemoteChatWithConversationKey(
            chat: Chat,
            participantUserIds: List<String>,
            currentUserId: String?,
        ): RemoteChatRoom {
            val identity = deviceIdentityProvider.getOrCreateDeviceIdentity()
            val privateKey = identity.privateKey ?: error("Current device private key not found")
            val devices = loadParticipantDevices(participantUserIds)

            val missingDeviceUserIds = participantUserIds.filter { userId -> devices.none { it.userId == userId } }
            check(missingDeviceUserIds.isEmpty()) { "No devices found for participants: ${missingDeviceUserIds.joinToString()}" }

            val remoteChat =
                chatRemoteDataSource.createChatRoom(
                    CreateChatRoomRequest(
                        type = chat.type,
                        name = chat.name.takeIf { chat.type == ChatType.GROUP && it.isNotBlank() },
                        participantIds = participantUserIds.filterNot { it == currentUserId },
                    ),
                )

            val conversationKey = cryptoService.generateConversationKey()
            val keyVersion = INITIAL_KEY_VERSION
            devices.forEach { device ->
                val encryptedKey =
                    cryptoService.encryptConversationKeyForUser(
                        conversationKey = conversationKey,
                        userPublicKey = device.publicKey,
                        myPrivateKey = privateKey,
                    )
                chatRemoteDataSource.uploadConversationKey(
                    UploadConversationKeyRequest(
                        conversationId = remoteChat.id,
                        targetUserId = checkNotNull(device.userId),
                        targetDeviceId = device.deviceId,
                        senderDeviceId = identity.deviceId,
                        keyVersion = keyVersion,
                        ciphertext = encryptedKey.ciphertext,
                        iv = encryptedKey.iv,
                    ),
                )
            }

            conversationKeyDao.upsert(
                ConversationKeyEntity(
                    conversationId = remoteChat.id,
                    keyVersion = keyVersion,
                    key = conversationKey,
                    createdAt = Instant.now().toString(),
                ),
            )
            return remoteChat
        }

        private suspend fun loadParticipantDevices(participantUserIds: List<String>): List<DevicePublicKey> =
            participantUserIds.flatMap { userId ->
                runCatching { chatRemoteDataSource.getUserDeviceKeys(userId).devices }
                    .onFailure { throwable -> logger.e(TAG, "Failed to load device keys userId=$userId", throwable) }
                    .getOrDefault(emptyList())
            }

        private suspend fun upsertRemoteChat(remoteChat: RemoteChatRoom): Long {
            val existingId = dao.findIdByRemoteId(remoteChat.id)
            val chatId =
                if (existingId == null) {
                    dao.insert(remoteChat.toEntity())
                } else {
                    dao.updateRemoteMetadata(
                        chatId = existingId,
                        remoteId = remoteChat.id,
                        name = remoteChat.name,
                        type = remoteChat.type,
                        createdAt = remoteChat.createdAt,
                        updatedAt = remoteChat.updatedAt,
                    )
                    existingId
                }

            ensureParticipantUsers(remoteChat.participants.map { it.userId })
            dao.insertParticipants(
                remoteChat.participants.map { participant ->
                    ChatParticipantCrossRef(
                        chatId = chatId,
                        userId = participant.userId,
                        remoteParticipantId = participant.id,
                    )
                },
            )
            return chatId
        }

        private fun RemoteChatRoom.toEntity(): ChatEntity =
            ChatEntity(
                remoteId = id,
                type = type,
                name = name,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private suspend fun ensureParticipantUsers(userIds: List<String>) {
            val distinctIds = userIds.distinct().filter { it.isNotBlank() }
            if (distinctIds.isEmpty()) return

            val existingIds = userDao.getUsersByIds(distinctIds).map { it.id }.toSet()
            val missingUsers =
                distinctIds
                    .filterNot { it in existingIds }
                    .map { userId ->
                        UserEntity(
                            id = userId,
                            email = userId,
                            username = userId,
                        )
                    }
            if (missingUsers.isNotEmpty()) {
                userDao.upsertAll(missingUsers)
            }
        }

        private suspend fun ChatListItemEntity.withRepairedDirectParticipant(currentUserId: String?): ChatListItemEntity {
            if (chat.type != ChatType.DIRECT || currentUserId == null || participants.size != SINGLE_DIRECT_PARTICIPANT) {
                return this
            }
            if (participants.none { participant -> participant.id == currentUserId }) return this

            val peer =
                userDao
                    .getUserByUsername(chat.name)
                    ?.takeIf { user -> user.id != currentUserId }
                    ?: return this

            dao.insertParticipants(listOf(ChatParticipantCrossRef(chatId = chat.id, userId = peer.id)))
            return copy(participants = participants + peer)
        }

        private suspend fun ChatListItemSummary.withResolvedAvatar(currentUserId: String?): ChatListItemSummary {
            if (!localProfilePicturePath.isNullOrBlank()) return this

            val remoteUrl = remoteProfilePictureUrl?.takeIf { it.isNotBlank() } ?: return this
            val localPath = resolveLocalAvatarPath(remoteUrl) ?: return this

            when (type) {
                ChatType.DIRECT -> {
                    otherParticipantUserId(currentUserId)?.let { userId ->
                        userDao.updateLocalProfilePicturePath(userId, localPath)
                    }
                }
                ChatType.GROUP -> dao.updateLocalProfilePicturePath(id, localPath)
            }

            return copy(localProfilePicturePath = localPath)
        }

        private suspend fun resolveLocalAvatarPath(remoteUrl: String): String? {
            avatarLocalDataSource.cachedPathFor(remoteUrl)?.let { cachedPath -> return cachedPath }

            val filename = remoteUrl.toAvatarFilename() ?: return null
            val bytes = userRemoteDataSource.downloadAvatar(filename) ?: return null
            return avatarLocalDataSource.saveAvatar(remoteUrl, bytes)
        }

        private fun ChatListItemSummary.otherParticipantUserId(currentUserId: String?): String? =
            participantUserIds.firstOrNull { participantUserId ->
                currentUserId != null && participantUserId != currentUserId
            } ?: participantUserIds.firstOrNull()

        private fun String.toAvatarFilename(): String? =
            substringBefore('?')
                .trimEnd('/')
                .substringAfterLast('/')
                .takeIf { it.isNotBlank() }
    }

private const val INITIAL_KEY_VERSION = 1
private const val SINGLE_DIRECT_PARTICIPANT = 1
private const val TAG = "ChatRepository"
