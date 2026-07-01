package com.citruschat.citrusmobile.domain.repository

import com.citruschat.citrusmobile.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeMessages(chatId: Long): Flow<List<Message>>

    fun observeTotalUnreadCount(): Flow<Int>

    suspend fun startRealtime(chatId: Long)

    suspend fun startRealtimeForChats(chatIds: List<Long>)

    suspend fun syncMessages(chatId: Long)

    suspend fun sendMessage(message: Message)

    fun stopRealtime()
}
