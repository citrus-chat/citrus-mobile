package com.citruschat.citrusmobile.ui.home

import com.citruschat.citrusmobile.domain.model.ChatListItemSummary

data class HomeUiState(
    val chats: List<ChatListItemSummary> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
