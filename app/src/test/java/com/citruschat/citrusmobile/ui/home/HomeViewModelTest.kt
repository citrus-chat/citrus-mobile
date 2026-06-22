package com.citruschat.citrusmobile.ui.home

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.local.entity.type.ChatType
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.model.User
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import com.citruschat.citrusmobile.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `selecting user creates direct chat with current user and peer participants`() =
        runTest {
            val chatRepository = FakeChatRepository(createdChatId = 99)
            val userRepository =
                FakeUserRepository(
                    currentUser = User(id = "current-user", email = "me@example.com", username = "me"),
                )
            val viewModel = HomeViewModel(chatRepository, userRepository, NoOpLogger)
            val event =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    assertEquals(99L, viewModel.openChatEvents.first())
                }

            viewModel.onUserResultClick(User(id = "user-3", email = "linus@example.com", username = "linus"))
            advanceUntilIdle()

            assertEquals(listOf(listOf("current-user", "user-3")), chatRepository.directChatLookups)
            assertEquals(
                listOf(
                    Chat(
                        name = "linus",
                        type = ChatType.DIRECT,
                        participantUserIds = listOf("current-user", "user-3"),
                    ),
                ),
                chatRepository.createdChats,
            )
            assertEquals(listOf("user-3"), userRepository.savedUsers.map { user -> user.id })
            event.cancel()
        }

    @Test
    fun `selecting current user does not create one participant direct chat`() =
        runTest {
            val chatRepository = FakeChatRepository(createdChatId = 99)
            val userRepository =
                FakeUserRepository(
                    currentUser = User(id = "current-user", email = "me@example.com", username = "me"),
                )
            val logger = CapturingLogger()
            val viewModel = HomeViewModel(chatRepository, userRepository, logger)

            viewModel.onUserResultClick(User(id = "current-user", email = "me@example.com", username = "me"))
            advanceUntilIdle()

            assertEquals(emptyList<List<String>>(), chatRepository.directChatLookups)
            assertEquals(emptyList<Chat>(), chatRepository.createdChats)
            assertEquals(listOf("current-user"), userRepository.savedUsers.map { user -> user.id })
            assertEquals(1, logger.warnings.count { message -> message.contains("participants are not distinct") })
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeChatRepository(
    private val existingDirectChatId: Long? = null,
    private val createdChatId: Long = 1,
) : ChatRepository {
    private val chats = MutableStateFlow<List<ChatListItemSummary>>(emptyList())
    val observedQueries = mutableListOf<String>()
    val directChatLookups = mutableListOf<List<String>>()
    val createdChats = mutableListOf<Chat>()

    override fun observeChatsItems(searchQuery: String): Flow<List<ChatListItemSummary>> {
        observedQueries += searchQuery
        return chats
    }

    override suspend fun findDirectChatId(participantUserIds: List<String>): Long? {
        directChatLookups += participantUserIds
        return existingDirectChatId
    }

    override suspend fun createChat(chat: Chat): Long {
        createdChats += chat
        return createdChatId
    }

    override suspend fun deleteChat(chatId: Long) = Unit

    fun emitChats(items: List<ChatListItemSummary>) {
        chats.update { items }
    }
}

private class FakeUserRepository(
    currentUser: User? = null,
    private val searchResults: List<User> = emptyList(),
) : UserRepository {
    private val currentUserState = MutableStateFlow(currentUser)
    val searchQueries = mutableListOf<String>()
    val savedUsers = mutableListOf<User>()

    override fun observeCurrentUser(): Flow<User?> = currentUserState

    override suspend fun searchUsers(query: String): List<User> {
        searchQueries += query
        return searchResults
    }

    override suspend fun refreshCurrentUser(): User? = currentUserState.value

    override suspend fun getAvatarLocalPath(user: User): String? = user.localProfilePicturePath

    override suspend fun getAvatarLocalPath(avatarUrl: String?): String? = null

    override suspend fun uploadCurrentUserAvatar(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): User? = currentUserState.value

    override suspend fun saveCurrentUser(user: User) {
        currentUserState.value = user.copy(isCurrentUser = true)
    }

    override suspend fun saveUsers(users: List<User>) {
        savedUsers += users
    }

    override suspend fun clearCurrentUser() {
        currentUserState.value = null
    }
}

private object NoOpLogger : Logger {
    override fun v(
        tag: String,
        message: String,
    ) = Unit

    override fun d(
        tag: String,
        message: String,
    ) = Unit

    override fun i(
        tag: String,
        message: String,
    ) = Unit

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = Unit

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = Unit
}

private class CapturingLogger : Logger {
    val warnings = mutableListOf<String>()

    override fun v(
        tag: String,
        message: String,
    ) = Unit

    override fun d(
        tag: String,
        message: String,
    ) = Unit

    override fun i(
        tag: String,
        message: String,
    ) = Unit

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        warnings += message
    }

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = Unit
}
