package com.citruschat.citrusmobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.model.User
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import com.citruschat.citrusmobile.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val chatRepository: ChatRepository,
        private val userRepository: UserRepository,
        private val logger: Logger,
    ) : ViewModel() {
        private val searchQuery = MutableStateFlow("")
        private val _openChatEvents = MutableSharedFlow<Long>()
        val openChatEvents = _openChatEvents.asSharedFlow()

        val uiState: StateFlow<HomeUiState> =
            searchQuery
                .flatMapLatest { rawQuery ->
                    val query = rawQuery.trim()
                    combine(
                        chatRepository.observeChatsItems(query),
                        observeUserResults(query),
                    ) { chats, users ->
                        logger.d(
                            TAG,
                            "Search loaded chats=${chats.size} users=${users.size} query=$query",
                        )
                        HomeUiState(
                            chats = chats,
                            userResults = users.withoutExistingDirectChats(chats),
                            searchQuery = rawQuery,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }.onStart {
                        logger.i(TAG, "Search loading started query=$query")
                        emit(HomeUiState(searchQuery = rawQuery, isLoading = true))
                    }
                }.catch { t ->
                    logger.e(TAG, "Search loading failed", t)
                    emit(
                        HomeUiState(
                            chats = emptyList(),
                            userResults = emptyList(),
                            searchQuery = searchQuery.value,
                            isLoading = false,
                            errorMessage = t.message ?: "Failed to load chats",
                        ),
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = HomeUiState(isLoading = true),
                )

        fun onSearchQueryChange(query: String) {
            searchQuery.value = query
        }

        fun onUserResultClick(user: User) {
            viewModelScope.launch {
                logger.i(TAG, "User search result selected id=${user.id}")
                userRepository.saveUsers(listOf(user))

                val currentUser = userRepository.observeCurrentUser().firstOrNull()
                val participantUserIds =
                    listOfNotNull(currentUser?.id, user.id)
                        .distinct()
                val chatId =
                    chatRepository.findDirectChatId(participantUserIds)
                        ?: chatRepository.createChat(
                            Chat(
                                name = user.displayName,
                                participantUserIds = participantUserIds,
                            ),
                        )

                _openChatEvents.emit(chatId)
            }
        }

        private fun observeUserResults(query: String): Flow<List<User>> =
            flow {
                if (query.isBlank()) {
                    emit(emptyList())
                } else {
                    emit(userRepository.searchUsers(query))
                }
            }.onStart {
                emit(emptyList())
            }.catch { t ->
                logger.e(TAG, "User search failed", t)
                emit(emptyList())
            }

        private fun List<User>.withoutExistingDirectChats(chats: List<ChatListItemSummary>): List<User> =
            filterNot { user ->
                chats.any { chat ->
                    user.id in chat.participantUserIds && chat.participantUserIds.size <= DIRECT_CHAT_MAX_PARTICIPANTS
                }
            }
    }

private val User.displayName: String
    get() = username.ifBlank { email.ifBlank { id } }

private const val DIRECT_CHAT_MAX_PARTICIPANTS = 2
private const val TAG = "HomeViewModel"
