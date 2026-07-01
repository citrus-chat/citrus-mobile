package com.citruschat.citrusmobile.domain.repository

import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatDetails
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChatsItems(searchQuery: String = ""): Flow<List<ChatListItemSummary>>

    fun observeChatDetails(chatId: Long): Flow<ChatDetails?>

    suspend fun findDirectChatId(participantUserIds: List<String>): Long?

    suspend fun createChat(chat: Chat): Long

    suspend fun syncChats()

    suspend fun deleteChat(chatId: Long)
}
