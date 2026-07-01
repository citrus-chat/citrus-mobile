package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.chat.ChatApiClient
import com.citruschat.citrusmobile.data.chat.SyncedChatRoom
import com.citruschat.citrusmobile.data.chat.SyncedConversationKey
import com.citruschat.citrusmobile.data.chat.UploadConversationKey
import com.citruschat.citrusmobile.data.crypto.ConversationCrypto
import com.citruschat.citrusmobile.data.crypto.ConversationKeyStore
import com.citruschat.citrusmobile.data.crypto.EncryptedPayload
import com.citruschat.citrusmobile.data.device.DeviceIdentityProvider
import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.local.dao.UserDao
import com.citruschat.citrusmobile.data.local.entity.ChatListItemEntity
import com.citruschat.citrusmobile.data.local.entity.ChatParticipantCrossRef
import com.citruschat.citrusmobile.data.local.entity.UserEntity
import com.citruschat.citrusmobile.data.local.entity.type.ChatType
import com.citruschat.citrusmobile.data.mapper.toDetails
import com.citruschat.citrusmobile.data.mapper.toEntity
import com.citruschat.citrusmobile.data.mapper.toParticipantRefs
import com.citruschat.citrusmobile.data.mapper.toSummary
import com.citruschat.citrusmobile.data.user.UserAvatarLocalDataSource
import com.citruschat.citrusmobile.data.user.UserRemoteDataSource
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatDetails
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class ChatRepositoryImpl
    @Inject
    constructor(
        private val dao: ChatDao,
        private val userDao: UserDao,
        private val userRemoteDataSource: UserRemoteDataSource,
        private val avatarLocalDataSource: UserAvatarLocalDataSource,
        private val chatApiClient: ChatApiClient,
        private val deviceIdentityProvider: DeviceIdentityProvider,
        private val conversationCrypto: ConversationCrypto,
        private val conversationKeyStore: ConversationKeyStore,
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

        override fun observeChatDetails(chatId: Long): Flow<ChatDetails?> =
            combine(
                dao.observeItem(chatId),
                userDao.observeCurrentUser(),
            ) { item, currentUser ->
                val currentUserId = currentUser?.id
                item
                    ?.withRepairedDirectParticipant(currentUserId)
                    ?.toDetails(currentUserId)
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
            logger.i(TAG, "Creating chat id=${chat.id} name=${chat.name}")
            val remoteChatId = chat.remoteId ?: createRemoteChat(chat)
            val chatToPersist = chat.copy(remoteId = remoteChatId)
            val chatId =
                runCatching {
                    dao.insert(chatToPersist.toEntity())
                }.onFailure { throwable ->
                    logger.e(TAG, "Create chat failed id=${chat.id}", throwable)
                    throw throwable
                }.getOrThrow()

            val participantRefs = chatToPersist.toParticipantRefs(chatId = chatId)
            if (participantRefs.isNotEmpty()) {
                dao.insertParticipants(participantRefs)
            }
            logger.d(TAG, "Chat created: $chat")
            return chatId
        }

        override suspend fun syncChats() {
            runCatching {
                val device = deviceIdentityProvider.getOrCreateDeviceIdentity()
                val sync = chatApiClient.syncChatRooms(device.deviceId)
                importConversationKeys(sync.conversationKeys, device.privateKey)
                sync.chatRooms.forEach { chatRoom ->
                    importChatRoom(chatRoom)
                }
            }.onFailure { throwable ->
                logger.e(TAG, "Chat sync failed", throwable)
            }
        }

        private suspend fun importConversationKeys(
            keys: List<SyncedConversationKey>,
            privateKey: String,
        ) {
            keys.forEach { key ->
                if (key.keyVersion < INITIAL_KEY_VERSION) return@forEach
                if (conversationKeyStore.getKey(key.conversationId, key.keyVersion) != null) return@forEach

                val senderPublicKey = chatApiClient.getDevicePublicKey(key.senderDeviceId)?.publicKey
                if (senderPublicKey.isNullOrBlank()) {
                    logger.w(TAG, "Skipping conversation key because sender device key is missing deviceId=${key.senderDeviceId}")
                    return@forEach
                }

                runCatching {
                    conversationCrypto.decryptConversationKeyFromDevice(
                        payload = EncryptedPayload(iv = key.iv, ciphertext = key.ciphertext),
                        senderPublicKey = senderPublicKey,
                        targetPrivateKey = privateKey,
                    )
                }.onSuccess { conversationKey ->
                    conversationKeyStore.saveKey(
                        conversationId = key.conversationId,
                        keyVersion = key.keyVersion,
                        key = conversationKey,
                    )
                }.onFailure { throwable ->
                    logger.e(TAG, "Failed to decrypt conversation key conversationId=${key.conversationId}", throwable)
                }
            }
        }

        private suspend fun importChatRoom(chatRoom: SyncedChatRoom) {
            val participantUserIds = chatRoom.participantUserIds.distinct()
            ensureParticipantUsers(participantUserIds)

            val existingChatId = dao.chatIdForRemoteId(chatRoom.id)
            val chatId =
                existingChatId
                    ?: dao.insert(
                        Chat(
                            remoteId = chatRoom.id,
                            name = chatRoom.name.ifBlank { chatRoom.id },
                            type = chatRoom.type.toChatType(),
                            participantUserIds = participantUserIds,
                        ).toEntity(),
                    )

            val participantRefs =
                participantUserIds.map { userId ->
                    ChatParticipantCrossRef(chatId = chatId, userId = userId)
                }
            if (participantRefs.isNotEmpty()) {
                dao.insertParticipants(participantRefs)
            }
        }

        private suspend fun ensureParticipantUsers(userIds: List<String>) {
            if (userIds.isEmpty()) return
            val existingIds = userDao.getUsersByIds(userIds).map { user -> user.id }.toSet()
            val missingUsers =
                userIds
                    .filterNot { userId -> userId in existingIds }
                    .map { userId ->
                        UserEntity(
                            id = userId,
                            email = "",
                            username = userId.toDisplayFallback(),
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

        private suspend fun createRemoteChat(chat: Chat): String? {
            val currentUserId = userDao.observeCurrentUser().firstOrNull()?.id
            val remoteParticipantIds =
                chat.participantUserIds
                    .filterNot { participantId -> currentUserId != null && participantId == currentUserId }
                    .distinct()

            if (chat.type == ChatType.DIRECT && remoteParticipantIds.size != DIRECT_REMOTE_PARTICIPANT_COUNT) {
                logger.w(TAG, "Remote direct chat creation skipped because participant count=${remoteParticipantIds.size}")
                return null
            }

            val remoteChatId =
                chatApiClient.createChatRoom(
                    type = chat.type,
                    name = chat.name,
                    participantIds = remoteParticipantIds,
                ) ?: return null

            distributeConversationKey(
                remoteChatId = remoteChatId,
                participantUserIds = chat.participantUserIds.distinct(),
            )
            return remoteChatId
        }

        private suspend fun distributeConversationKey(
            remoteChatId: String,
            participantUserIds: List<String>,
        ) {
            val senderDevice = deviceIdentityProvider.getOrCreateDeviceIdentity()
            val conversationKey = conversationCrypto.generateConversationKey()
            val keyVersion = INITIAL_KEY_VERSION

            participantUserIds.forEach { userId ->
                chatApiClient.getUserDevices(userId).forEach { targetDevice ->
                    val encryptedKey =
                        conversationCrypto.encryptConversationKeyForDevice(
                            conversationKey = conversationKey,
                            targetPublicKey = targetDevice.publicKey,
                            senderPrivateKey = senderDevice.privateKey,
                        )
                    chatApiClient.uploadConversationKey(
                        UploadConversationKey(
                            conversationId = remoteChatId,
                            targetUserId = userId,
                            targetDeviceId = targetDevice.deviceId,
                            senderDeviceId = senderDevice.deviceId,
                            keyVersion = keyVersion,
                            ciphertext = encryptedKey.ciphertext,
                            iv = encryptedKey.iv,
                        ),
                    )
                }
            }

            conversationKeyStore.saveKey(
                conversationId = remoteChatId,
                keyVersion = keyVersion,
                key = conversationKey,
            )
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

        private fun String.toChatType(): ChatType =
            runCatching { ChatType.valueOf(uppercase()) }
                .getOrDefault(ChatType.GROUP)

        private fun String.toDisplayFallback(): String = take(DISPLAY_ID_PREFIX_LENGTH).ifBlank { "Unknown" }

        private fun String.toAvatarFilename(): String? =
            substringBefore('?')
                .trimEnd('/')
                .substringAfterLast('/')
                .takeIf { it.isNotBlank() }
    }

private const val SINGLE_DIRECT_PARTICIPANT = 1
private const val DIRECT_REMOTE_PARTICIPANT_COUNT = 1
private const val INITIAL_KEY_VERSION = 1
private const val DISPLAY_ID_PREFIX_LENGTH = 8
private const val TAG = "ChatRepository"
