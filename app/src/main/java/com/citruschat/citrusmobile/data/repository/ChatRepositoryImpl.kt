package com.citruschat.citrusmobile.data.repository

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
    ) : ChatRepository {
        override fun observeChatsItems(): Flow<List<ChatListItemSummary>> =
            dao
                .observeAllItems()
                .map { entities -> entities.map { it.toSummary() } }

        override suspend fun createChat(chat: Chat) {
            dao.insert(chat.toEntity())
        }

        override suspend fun deleteChat(chatId: Long) {
            dao.deleteById(chatId)
        }
    }
