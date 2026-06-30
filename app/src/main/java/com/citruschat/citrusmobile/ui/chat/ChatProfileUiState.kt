package com.citruschat.citrusmobile.ui.chat

import com.citruschat.citrusmobile.domain.model.ChatDetails

data class ChatProfileUiState(
    val chat: ChatDetails? = null,
    val isLoading: Boolean = false,
)
