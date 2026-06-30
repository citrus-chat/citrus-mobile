package com.citruschat.citrusmobile.ui.chat

import com.citruschat.citrusmobile.domain.model.Message
import com.citruschat.citrusmobile.domain.model.ChatDetails

data class ChatUiState(
    val chat: ChatDetails? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)
