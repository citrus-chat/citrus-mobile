package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.mapper.toEntity
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl
    @Inject
    constructor(
        private val dao: ChatDao,
    ) : ChatRepository {
        override suspend fun createChat(chat: Chat) {
            dao.insert(chat.toEntity())
        }
    }
