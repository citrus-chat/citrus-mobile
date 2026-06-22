package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.local.dao.UserDao
import com.citruschat.citrusmobile.data.local.entity.ChatListItemEntity
import com.citruschat.citrusmobile.data.local.entity.ChatParticipantCrossRef
import com.citruschat.citrusmobile.data.local.entity.type.ChatType
import com.citruschat.citrusmobile.data.mapper.toEntity
import com.citruschat.citrusmobile.data.mapper.toParticipantRefs
import com.citruschat.citrusmobile.data.mapper.toSummary
import com.citruschat.citrusmobile.data.user.UserAvatarLocalDataSource
import com.citruschat.citrusmobile.data.user.UserRemoteDataSource
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ChatRepositoryImpl
    @Inject
    constructor(
        private val dao: ChatDao,
        private val userDao: UserDao,
        private val userRemoteDataSource: UserRemoteDataSource,
        private val avatarLocalDataSource: UserAvatarLocalDataSource,
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
            logger.i(TAG, "Creating chat id=${chat.id} name=${chat.name}")
            val chatId =
                runCatching {
                    dao.insert(chat.toEntity())
                }.onFailure { throwable ->
                    logger.e(TAG, "Create chat failed id=${chat.id}", throwable)
                    throw throwable
                }.getOrThrow()

            val participantRefs = chat.toParticipantRefs(chatId = chatId)
            if (participantRefs.isNotEmpty()) {
                dao.insertParticipants(participantRefs)
            }
            logger.d(TAG, "Chat created: $chat")
            return chatId
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

        private fun String.toAvatarFilename(): String? =
            substringBefore('?')
                .trimEnd('/')
                .substringAfterLast('/')
                .takeIf { it.isNotBlank() }
    }

private const val SINGLE_DIRECT_PARTICIPANT = 1
private const val TAG = "ChatRepository"
