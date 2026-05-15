package com.citruschat.citrusmobile.domain.repository

import com.citruschat.citrusmobile.domain.model.Chat

interface ChatRepository {
    suspend fun createChat(chat: Chat)
}
