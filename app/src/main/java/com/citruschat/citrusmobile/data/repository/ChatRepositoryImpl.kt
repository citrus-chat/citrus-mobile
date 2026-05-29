package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.mapper.toEntity
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
        override fun observeChatsItems(): Flow<List<ChatListItemSummary>> =
            dao
                .observeAllItems()
                .map { entities ->
                    logger.v(TAG, "Observed chat list update with count=${entities.size}")
                    entities.map { it.toSummary() }
                }

        override suspend fun createChat(chat: Chat) {
            logger.i(TAG, "Creating chat id=${chat.id} name=${chat.name}")
            runCatching {
                dao.insert(chat.toEntity())
            }.onFailure { throwable ->
                logger.e(TAG, "Create chat failed id=${chat.id}", throwable)
                throw throwable
            }
            logger.d(TAG, "Chat created id=${chat.id}")
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
