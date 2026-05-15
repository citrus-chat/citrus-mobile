package com.citruschat.citrusmobile.ui.home

import com.citruschat.citrusmobile.domain.model.Chat

data class HomeUiState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
