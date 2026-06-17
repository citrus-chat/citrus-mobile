package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.mapper.toEntity
import com.citruschat.citrusmobile.data.mapper.toParticipantRefs
import com.citruschat.citrusmobile.data.mapper.toSummary
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl
    @Inject
    constructor(
        private val dao: ChatDao,
        private val logger: Logger,
    ) : ChatRepository {
        override fun observeChatsItems(searchQuery: String): Flow<List<ChatListItemSummary>> =
            dao
                .observeItems(searchQuery.trim())
                .map { entities ->
                    logger.v(
                        TAG,
                        "Observed chat list update with count=${entities.size} searchQuery=$searchQuery",
                    )
                    entities.map { it.toSummary() }
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
            logger.d(TAG, "Chat created id=$chatId participants=${participantRefs.size}")
            return chatId
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
    }

private const val TAG = "ChatRepository"
