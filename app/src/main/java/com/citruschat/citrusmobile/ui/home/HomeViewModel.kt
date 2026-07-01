package com.citruschat.citrusmobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.local.entity.type.ChatType
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.model.User
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import com.citruschat.citrusmobile.domain.repository.MessageRepository
import com.citruschat.citrusmobile.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val chatRepository: ChatRepository,
        private val messageRepository: MessageRepository,
        private val userRepository: UserRepository,
        private val logger: Logger,
    ) : ViewModel() {
        private val searchQuery = MutableStateFlow("")
        private val _openChatEvents = MutableSharedFlow<Long>()
        val openChatEvents = _openChatEvents.asSharedFlow()

        init {
            viewModelScope.launch {
                chatRepository.syncChats()
            }
            viewModelScope.launch {
                chatRepository
                    .observeChatsItems()
                    .map { chats -> chats.map { chat -> chat.id } }
                    .distinctUntilChanged()
                    .collect(messageRepository::startRealtimeForChats)
            }
        }

        private val searchResults: Flow<SearchResults> =
            searchQuery
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .map { rawQuery -> rawQuery.trim() }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    when {
                        query.isBlank() -> observeSearchResults(query)
                        query.length > MIN_SEARCH_QUERY_LENGTH -> observeSearchResults(query)
                        else -> flowOf(SearchResults(query = query))
                    }
                }.catch { t ->
                    logger.e(TAG, "Search loading failed", t)
                    emit(
                        SearchResults(
                            query = searchQuery.value.trim(),
                            errorMessage = t.message ?: "Failed to load chats",
                        ),
                    )
                }

        val uiState: StateFlow<HomeUiState> =
            combine(searchQuery, searchResults) { rawQuery, results ->
                val typedQuery = rawQuery.trim()
                val shouldShowResults = typedQuery.isBlank() || typedQuery.length > MIN_SEARCH_QUERY_LENGTH

                if (shouldShowResults && typedQuery == results.query) {
                    HomeUiState(
                        chats = results.chats,
                        userResults = results.users.withoutExistingDirectChats(results.chats),
                        searchQuery = rawQuery,
                        isLoading = results.isLoading,
                        errorMessage = results.errorMessage,
                    )
                } else {
                    HomeUiState(searchQuery = rawQuery)
                }
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
                if (participantUserIds.size < DIRECT_CHAT_MAX_PARTICIPANTS) {
                    logger.w(
                        TAG,
                        "Skipping direct chat creation because participants are not distinct currentUserId=${currentUser?.id} selectedUserId=${user.id}",
                    )
                    return@launch
                }

                val chatId =
                    chatRepository.findDirectChatId(participantUserIds)
                        ?: chatRepository.createChat(
                            Chat(
                                name = user.displayName,
                                participantUserIds = participantUserIds,
                                type = ChatType.DIRECT,
                            ),
                        )

                _openChatEvents.emit(chatId)
            }
        }

        private fun observeSearchResults(query: String): Flow<SearchResults> =
            combine(
                chatRepository.observeChatsItems(query),
                observeUserResults(query),
            ) { chats, users ->
                logger.d(
                    TAG,
                    "Search loaded chats=${chats.size} users=${users.size} query=$query",
                )
                SearchResults(
                    query = query,
                    chats = chats,
                    users = users,
                )
            }.onStart {
                logger.i(TAG, "Search loading started query=$query")
                emit(SearchResults(query = query, isLoading = true))
            }

        private fun observeUserResults(query: String): Flow<List<User>> =
            flow {
                if (query.length > MIN_SEARCH_QUERY_LENGTH) {
                    emit(userRepository.searchUsers(query))
                } else {
                    emit(emptyList())
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

private data class SearchResults(
    val query: String,
    val chats: List<ChatListItemSummary> = emptyList(),
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

private val User.displayName: String
    get() = username.ifBlank { email.ifBlank { id } }

private const val SEARCH_DEBOUNCE_MILLIS = 350L
private const val MIN_SEARCH_QUERY_LENGTH = 2
private const val DIRECT_CHAT_MAX_PARTICIPANTS = 2
private const val TAG = "HomeViewModel"
