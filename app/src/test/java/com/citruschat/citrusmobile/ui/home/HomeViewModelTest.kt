private fun HomeViewModelTest() {
    return
}

// package com.citruschat.citrusmobile.ui.home
//
// import com.citruschat.citrusmobile.core.logging.Logger
// import com.citruschat.citrusmobile.domain.model.Chat
// import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
// import com.citruschat.citrusmobile.domain.model.User
// import com.citruschat.citrusmobile.domain.repository.ChatRepository
// import com.citruschat.citrusmobile.domain.repository.UserRepository
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.ExperimentalCoroutinesApi
// import kotlinx.coroutines.flow.Flow
// import kotlinx.coroutines.flow.MutableStateFlow
// import kotlinx.coroutines.flow.first
// import kotlinx.coroutines.flow.update
// import kotlinx.coroutines.launch
// import kotlinx.coroutines.test.StandardTestDispatcher
// import kotlinx.coroutines.test.TestDispatcher
// import kotlinx.coroutines.test.UnconfinedTestDispatcher
// import kotlinx.coroutines.test.advanceUntilIdle
// import kotlinx.coroutines.test.resetMain
// import kotlinx.coroutines.test.runTest
// import kotlinx.coroutines.test.setMain
// import org.junit.Assert.assertEquals
// import org.junit.Rule
// import org.junit.Test
// import org.junit.rules.TestWatcher
// import org.junit.runner.Description
//
// @OptIn(ExperimentalCoroutinesApi::class)
// class HomeViewModelTest {
//    @get:Rule
//    internal val mainDispatcherRule = MainDispatcherRule()
//
//    @Test
//    fun `search exposes existing chats first and remote user results last`() =
//        runTest {
//            val chatRepository = FakeChatRepository()
//            val userRepository =
//                FakeUserRepository(
//                    searchResults =
//                        listOf(
//                            User(id = "user-2", email = "grace@example.com", username = "grace"),
//                            User(id = "user-3", email = "linus@example.com", username = "linus"),
//                        ),
//                )
//            val viewModel = HomeViewModel(chatRepository, userRepository, NoOpLogger)
//            val collection =
//                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
//                    viewModel.uiState.collect {}
//                }
//            chatRepository.emitChats(
//                listOf(
//                    ChatListItemSummary(
//                        id = 42,
//                        name = "Grace chat",
//                        lastMessagePreview = "hello",
//                        participantUserIds = listOf("current-user", "user-2"),
//                        participantUsernames = listOf("me", "grace"),
//                    ),
//                ),
//            )
//
//            viewModel.onSearchQueryChange("gra")
//            advanceUntilIdle()
//
//            val state = viewModel.uiState.value
//            assertEquals(listOf(42L), state.chats.map { it.id })
//            assertEquals(listOf("linus"), state.userResults.map { it.username })
//            assertEquals(listOf("gra"), chatRepository.observedQueries.takeLast(1))
//            assertEquals(listOf("gra"), userRepository.searchQueries)
//            collection.cancel()
//        }
//
//    @Test
//    fun `selecting user opens existing direct chat when one exists`() =
//        runTest {
//            val chatRepository = FakeChatRepository(existingDirectChatId = 7)
//            val userRepository = FakeUserRepository(
//                currentUser = User(id = "current-user", email = "me@example.com", username = "me")
//            )
//            val viewModel = HomeViewModel(chatRepository, userRepository, NoOpLogger)
//            val event =
//                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
//                    assertEquals(7L, viewModel.openChatEvents.first())
//                }
//
//            viewModel.onUserResultClick(User(id = "user-2", email = "grace@example.com", username = "grace"))
//            advanceUntilIdle()
//
//            assertEquals(listOf(listOf("current-user", "user-2")), chatRepository.directChatLookups)
//            assertEquals(emptyList<Chat>(), chatRepository.createdChats)
//            assertEquals(listOf("user-2"), userRepository.savedUsers.map { it.id })
//            event.cancel()
//        }
//
//    @Test
//    fun `selecting user creates direct chat and opens it when no chat exists`() =
//        runTest {
//            val chatRepository = FakeChatRepository(createdChatId = 99)
//            val userRepository = FakeUserRepository(
//                currentUser = User(id = "current-user", email = "me@example.com", username = "me")
//            )
//            val viewModel = HomeViewModel(chatRepository, userRepository, NoOpLogger)
//            val event =
//                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
//                    assertEquals(99L, viewModel.openChatEvents.first())
//                }
//
//            viewModel.onUserResultClick(User(id = "user-3", email = "linus@example.com", username = "linus"))
//            advanceUntilIdle()
//
//            assertEquals(listOf(listOf("current-user", "user-3")), chatRepository.directChatLookups)
//            assertEquals(
//                listOf(Chat(name = "linus", participantUserIds = listOf("current-user", "user-3"))),
//                chatRepository.createdChats,
//            )
//            event.cancel()
//        }
// }
//
// @OptIn(ExperimentalCoroutinesApi::class)
// private class MainDispatcherRule(
//    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
// ) : TestWatcher() {
//    override fun starting(description: Description) {
//        Dispatchers.setMain(dispatcher)
//    }
//
//    override fun finished(description: Description) {
//        Dispatchers.resetMain()
//    }
// }
//
// private class FakeChatRepository(
//    private val existingDirectChatId: Long? = null,
//    private val createdChatId: Long = 1,
// ) : ChatRepository {
//    private val chats = MutableStateFlow<List<ChatListItemSummary>>(emptyList())
//    val observedQueries = mutableListOf<String>()
//    val directChatLookups = mutableListOf<List<String>>()
//    val createdChats = mutableListOf<Chat>()
//
//    override fun observeChatsItems(searchQuery: String): Flow<List<ChatListItemSummary>> {
//        observedQueries += searchQuery
//        return chats
//    }
//
//    override suspend fun findDirectChatId(participantUserIds: List<String>): Long? {
//        directChatLookups += participantUserIds
//        return existingDirectChatId
//    }
//
//    override suspend fun createChat(chat: Chat): Long {
//        createdChats += chat
//        return createdChatId
//    }
//
//    override suspend fun deleteChat(chatId: Long) = Unit
//
//    fun emitChats(items: List<ChatListItemSummary>) {
//        chats.update { items }
//    }
// }
//
// private class FakeUserRepository(
//    currentUser: User? = null,
//    private val searchResults: List<User> = emptyList(),
// ) : UserRepository {
//    private val currentUserState = MutableStateFlow(currentUser)
//    val searchQueries = mutableListOf<String>()
//    val savedUsers = mutableListOf<User>()
//
//    override fun observeCurrentUser(): Flow<User?> = currentUserState
//
//    override suspend fun searchUsers(query: String): List<User> {
//        searchQueries += query
//        return searchResults
//    }
//
//    override suspend fun saveCurrentUser(user: User) {
//        currentUserState.value = user.copy(isCurrentUser = true)
//    }
//
//    override suspend fun saveUsers(users: List<User>) {
//        savedUsers += users
//    }
//
//    override suspend fun clearCurrentUser() {
//        currentUserState.value = null
//    }
// }
//
// private object NoOpLogger : Logger {
//    override fun v(
//        tag: String,
//        message: String,
//    ) = Unit
//
//    override fun d(
//        tag: String,
//        message: String,
//    ) = Unit
//
//    override fun i(
//        tag: String,
//        message: String,
//    ) = Unit
//
//    override fun w(
//        tag: String,
//        message: String,
//        throwable: Throwable?,
//    ) = Unit
//
//    override fun e(
//        tag: String,
//        message: String,
//        throwable: Throwable?,
//    ) = Unit
// }
// */
