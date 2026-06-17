package com.citruschat.citrusmobile.ui.home

import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.model.User

data class HomeUiState(
    val chats: List<ChatListItemSummary> = emptyList(),
    val userResults: List<User> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
