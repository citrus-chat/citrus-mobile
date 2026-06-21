package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.local.dao.MessageDao
import com.citruschat.citrusmobile.data.mapper.toDomain
import com.citruschat.citrusmobile.data.mapper.toEntity
import com.citruschat.citrusmobile.domain.model.Message
import com.citruschat.citrusmobile.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MessageRepositoryImpl
    @Inject
    constructor(
        private val dao: MessageDao,
        private val logger: Logger,
    ) : MessageRepository {
        override fun observeMessages(chatId: Long): Flow<List<Message>> =
            dao
                .observeByChatId(chatId)
                .map { entities ->
                    logger.v(TAG, "Observed messages update for chatId=$chatId count=${entities.size}")
                    entities.map { it.toDomain() }
                }

        override suspend fun sendMessage(message: Message) {
            logger.i(TAG, "Persisting message for chatId=${message.chatId} textLength=${message.text.length}")
            runCatching {
                dao.insertAndMarkAsLastMessage(message.toEntity())
            }.onFailure { throwable ->
                logger.e(TAG, "Message persist failed for chatId=${message.chatId}", throwable)
                throw throwable
            }
            logger.d(TAG, "Message persisted for chatId=${message.chatId}")
        }
    }

private const val TAG = "MessageRepository"
