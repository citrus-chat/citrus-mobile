package com.citruschat.citrusmobile.data.repository

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
    ) : MessageRepository {
        override fun observeMessages(chatId: Long): Flow<List<Message>> =
            dao
                .observeByChatId(chatId)
                .map { entities -> entities.map { it.toDomain() } }

        override suspend fun sendMessage(message: Message) {
            dao.insert(message.toEntity())
        }
    }
