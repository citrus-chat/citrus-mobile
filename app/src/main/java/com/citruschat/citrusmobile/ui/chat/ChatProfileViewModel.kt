package com.citruschat.citrusmobile.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class ChatProfileViewModel
    @Inject
    constructor(
        chatRepository: ChatRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val chatId: Long = checkNotNull(savedStateHandle["chatId"])

        val uiState: StateFlow<ChatProfileUiState> =
            chatRepository
                .observeChatDetails(chatId)
                .map { chat -> ChatProfileUiState(chat = chat, isLoading = chat == null) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = ChatProfileUiState(isLoading = true),
                )
    }
