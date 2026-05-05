package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.data.local.dao.MessageDao
import com.citruschat.citrusmobile.data.mapper.toDomain
import com.citruschat.citrusmobile.data.mapper.toEntity
import com.citruschat.citrusmobile.domain.model.Message
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl
    @Inject
    constructor(
        private val dao: MessageDao,
    ) : ChatRepository {
        override fun observeMessages(): Flow<List<Message>> =
            dao.observeMessages().map { list ->
                list.map { it.toDomain() }
            }

        override suspend fun sendMessage(message: Message) {
            dao.insert(message.toEntity())
        }
    }
