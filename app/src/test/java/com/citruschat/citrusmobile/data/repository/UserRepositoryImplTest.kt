package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.local.dao.UserDao
import com.citruschat.citrusmobile.data.local.entity.UserEntity
import com.citruschat.citrusmobile.data.user.UserRemoteDataSource
import com.citruschat.citrusmobile.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UserRepositoryImplTest {
    @Test
    fun `search users trims query saves remote results and returns them`() =
        runTest {
            val dao = FakeUserDao()
            val remote =
                FakeUserRemoteDataSource(
                    users = listOf(User(id = "user-1", email = "ada@example.com", username = "ada")),
                )
            val repository = UserRepositoryImpl(dao, remote, NoOpLogger)

            val users = repository.searchUsers("  ada  ")

            assertEquals("ada", remote.queries.single())
            assertEquals(remote.users, users)
            assertEquals(listOf("user-1"), dao.upsertedUsers.map { it.id })
        }

    @Test
    fun `search users returns empty list and skips remote for blank query`() =
        runTest {
            val dao = FakeUserDao()
            val remote = FakeUserRemoteDataSource(users = emptyList())
            val repository = UserRepositoryImpl(dao, remote, NoOpLogger)

            val users = repository.searchUsers("   ")

            assertEquals(emptyList<User>(), users)
            assertEquals(emptyList<String>(), remote.queries)
            assertEquals(emptyList<UserEntity>(), dao.upsertedUsers)
        }
}

private class FakeUserDao : UserDao {
    private val currentUser = MutableStateFlow<UserEntity?>(null)
    val upsertedUsers = mutableListOf<UserEntity>()

    override suspend fun upsert(user: UserEntity) {
        upsertedUsers += user
        if (user.isCurrentUser) currentUser.value = user
    }

    override suspend fun upsertAll(users: List<UserEntity>) {
        upsertedUsers += users
    }

    override fun observeCurrentUser(): Flow<UserEntity?> = currentUser

    override suspend fun clearCurrentUserFlag() {
        currentUser.value = null
    }
}

private class FakeUserRemoteDataSource(
    val users: List<User>,
) : UserRemoteDataSource {
    val queries = mutableListOf<String>()

    override suspend fun searchUsers(query: String): List<User> {
        queries += query
        return users
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
