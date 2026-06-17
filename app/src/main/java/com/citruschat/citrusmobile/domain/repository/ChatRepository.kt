package com.citruschat.citrusmobile.domain.repository

import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChatsItems(searchQuery: String = ""): Flow<List<ChatListItemSummary>>

    suspend fun findDirectChatId(participantUserIds: List<String>): Long?

    suspend fun createChat(chat: Chat): Long

    suspend fun deleteChat(chatId: Long)
}
