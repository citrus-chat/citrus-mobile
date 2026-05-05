package com.citruschat.citrusmobile.ui.state

import com.citruschat.citrusmobile.domain.model.Message

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)
