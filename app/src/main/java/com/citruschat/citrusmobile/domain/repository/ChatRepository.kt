package com.citruschat.citrusmobile.domain.repository

import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.model.ChatPermissions
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChatsItems(searchQuery: String = ""): Flow<List<ChatListItemSummary>>

    suspend fun findDirectChatId(participantUserIds: List<String>): Long?

    suspend fun createChat(chat: Chat): Long

    suspend fun getRemoteChatId(chatId: Long): String?

    suspend fun syncChats()

    suspend fun getPermissions(chatId: Long): ChatPermissions

    suspend fun deleteChat(chatId: Long)
}
