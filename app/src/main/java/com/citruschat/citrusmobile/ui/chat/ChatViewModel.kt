package com.citruschat.citrusmobile.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.domain.model.Message
import com.citruschat.citrusmobile.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel
    @Inject
    constructor(
        private val repository: MessageRepository,
        private val logger: Logger,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val chatId: Long = checkNotNull(savedStateHandle["chatId"])
        private val _inputText = MutableStateFlow("")
        val inputText = _inputText.asStateFlow()

        val messages: StateFlow<List<Message>> =
            repository
                .observeMessages(chatId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList(),
                )

        val uiState: StateFlow<ChatUiState> =
            combine(messages, inputText) { messages, input ->
                ChatUiState(
                    messages = messages,
                    inputText = input,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ChatUiState(isLoading = true),
            )

        init {
            logger.i(TAG, "Opened chatId=$chatId")
        }

        fun onInputChange(text: String) {
            _inputText.value = text
        }

        fun sendMessage() {
            val text = _inputText.value.trim()
            if (text.isBlank()) {
                logger.w(TAG, "Ignored empty message for chatId=$chatId")
                return
            }

            viewModelScope.launch {
                val newMessage =
                    Message(
                        id = 0, // let DB auto-generate if possible
                        user = "You",
                        text = text,
                        isOwn = true,
                        timestamp = System.currentTimeMillis(),
                        chatId = chatId,
                    )
                logger.d(TAG, "Sending message to chatId=$chatId text=$text")
                repository.sendMessage(newMessage)
                _inputText.value = ""
                logger.i(TAG, "Message sent to chatId=$chatId")
            }
        }
    }

private const val TAG = "ChatViewModel"
