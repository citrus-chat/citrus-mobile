package com.citruschat.citrusmobile.domain.repository

import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChatsItems(): Flow<List<ChatListItemSummary>>

    suspend fun createChat(chat: Chat)

    suspend fun deleteChat(chatId: Long)
}
