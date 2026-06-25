package com.citruschat.citrusmobile.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.BuildConfig
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.domain.model.ChatPermissions
import com.citruschat.citrusmobile.domain.model.Message
import com.citruschat.citrusmobile.domain.realtime.ChatRealtimeClient
import com.citruschat.citrusmobile.domain.realtime.ChatRealtimeEvent
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import com.citruschat.citrusmobile.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel
    @Inject
    constructor(
        private val repository: MessageRepository,
        private val chatRepository: ChatRepository,
        private val realtimeClient: ChatRealtimeClient,
        private val tokenStore: TokenStore,
        private val logger: Logger,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val chatId: Long = checkNotNull(savedStateHandle["chatId"])
        private val _inputText = MutableStateFlow("")
        private val permissions = MutableStateFlow(ChatPermissions())
        private val errorMessage = MutableStateFlow<String?>(null)
        private var subscribedRemoteChatId: String? = null
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
            combine(messages, inputText, permissions, errorMessage) { messages, input, permissions, error ->
                ChatUiState(
                    messages = messages,
                    inputText = input,
                    canSendMessages = permissions.canWrite,
                    error = error,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ChatUiState(isLoading = true),
            )

        init {
            logger.i(TAG, "Opened chatId=$chatId")
            viewModelScope.launch {
                runCatching { chatRepository.getPermissions(chatId) }
                    .onSuccess { permissions.value = it }
                    .onFailure { throwable -> logger.e(TAG, "Load chat permissions failed chatId=$chatId", throwable) }

                runCatching { repository.syncMessages(chatId) }
                    .onFailure { throwable ->
                        logger.e(TAG, "Message sync failed chatId=$chatId", throwable)
                        errorMessage.value = "Failed to sync messages"
                    }

                startRealtimeSync()
            }
        }

        private suspend fun startRealtimeSync() {
            val remoteChatId = chatRepository.getRemoteChatId(chatId) ?: return
            subscribedRemoteChatId = remoteChatId
            val accessToken = tokenStore.observeTokens().firstOrNull()?.accessToken
            realtimeClient.connect(REALTIME_URL, accessToken)
            realtimeClient.subscribeToChatRoom(remoteChatId)

            realtimeClient.events.collect { event ->
                if (event is ChatRealtimeEvent.ChatRoomMessage && event.chatRoomId == remoteChatId) {
                    runCatching { repository.syncMessages(chatId) }
                        .onFailure { throwable -> logger.e(TAG, "Realtime message sync failed chatId=$chatId", throwable) }
                }
            }
        }

        override fun onCleared() {
            subscribedRemoteChatId?.let { chatRoomId ->
                realtimeClient.unsubscribeFromChatRoom(chatRoomId)
            }
            super.onCleared()
        }

        fun onInputChange(text: String) {
            _inputText.value = text
            errorMessage.value = null
        }

        fun sendMessage() {
            val text = _inputText.value.trim()
            if (text.isBlank()) {
                logger.w(TAG, "Ignored empty message for chatId=$chatId")
                return
            }
            if (!permissions.value.canWrite) {
                logger.w(TAG, "Ignored message without send permission chatId=$chatId")
                errorMessage.value = "You do not have permission to send messages in this chat"
                return
            }

            viewModelScope.launch {
                val newMessage =
                    Message(
                        id = 0,
                        user = "You",
                        text = text,
                        isOwn = true,
                        timestamp = System.currentTimeMillis(),
                        chatId = chatId,
                    )
                logger.d(TAG, "Sending message to chatId=$chatId textLength=${text.length}")
                runCatching { repository.sendMessage(newMessage) }
                    .onSuccess {
                        _inputText.value = ""
                        errorMessage.value = null
                        logger.i(TAG, "Message sent to chatId=$chatId")
                    }.onFailure { throwable ->
                        logger.e(TAG, "Message send failed chatId=$chatId", throwable)
                        errorMessage.value = "Failed to send message"
                    }
            }
        }
    }

private val REALTIME_URL = "${BuildConfig.WS_BASE_URL.trimEnd('/')}/ws"
private const val TAG = "ChatViewModel"
