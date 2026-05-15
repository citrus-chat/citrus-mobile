package com.citruschat.citrusmobile.domain.repository

import com.citruschat.citrusmobile.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeMessages(chatId: Long): Flow<List<Message>>

    suspend fun sendMessage(message: Message)
}
