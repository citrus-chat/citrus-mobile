package com.citruschat.citrusmobile.domain.repository

import com.citruschat.citrusmobile.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeMessages(): Flow<List<Message>>

    suspend fun sendMessage(message: Message)
}
