package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.local.dao.UserDao
import com.citruschat.citrusmobile.data.local.entity.UserEntity
import com.citruschat.citrusmobile.data.user.UserAvatarLocalDataSource
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
            val repository = UserRepositoryImpl(dao, remote, FakeUserAvatarLocalDataSource(), NoOpLogger)

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
            val repository = UserRepositoryImpl(dao, remote, FakeUserAvatarLocalDataSource(), NoOpLogger)

            val users = repository.searchUsers("   ")

            assertEquals(emptyList<User>(), users)
            assertEquals(emptyList<String>(), remote.queries)
            assertEquals(emptyList<UserEntity>(), dao.upsertedUsers)
        }

    @Test
    fun `search users downloads avatar and stores local path for encountered users`() =
        runTest {
            val dao = FakeUserDao()
            val remote =
                FakeUserRemoteDataSource(
                    users = listOf(avatarUser()),
                    avatarBytes = byteArrayOf(1, 2, 3),
                )
            val repository = UserRepositoryImpl(dao, remote, FakeUserAvatarLocalDataSource(), NoOpLogger)

            val users = repository.searchUsers("ada")

            assertEquals("/local/${AVATAR_URL.hashCode()}.img", users.single().localProfilePicturePath)
            assertEquals("/local/${AVATAR_URL.hashCode()}.img", dao.upsertedUsers.single().localProfilePicturePath)
            assertEquals(listOf("avatar.png"), remote.downloadedAvatarFilenames)
        }

    @Test
    fun `get avatar local path returns cached path before remote download`() =
        runTest {
            val remote = FakeUserRemoteDataSource(users = emptyList())
            val avatars = FakeUserAvatarLocalDataSource(cachedPaths = mutableMapOf(AVATAR_URL to "/local/avatar.img"))
            val repository = UserRepositoryImpl(FakeUserDao(), remote, avatars, NoOpLogger)

            val path = repository.getAvatarLocalPath(avatarUser())

            assertEquals("/local/avatar.img", path)
            assertEquals(emptyList<String>(), remote.downloadedAvatarFilenames)
        }

    @Test
    fun `get avatar local path downloads and saves avatar on cache miss`() =
        runTest {
            val remote = FakeUserRemoteDataSource(users = emptyList(), avatarBytes = byteArrayOf(1, 2, 3))
            val avatars = FakeUserAvatarLocalDataSource()
            val repository = UserRepositoryImpl(FakeUserDao(), remote, avatars, NoOpLogger)

            val path = repository.getAvatarLocalPath(avatarUser())

            assertEquals("/local/${AVATAR_URL.hashCode()}.img", path)
            assertEquals(listOf("avatar.png"), remote.downloadedAvatarFilenames)
            assertEquals(byteArrayOf(1, 2, 3).toList(), avatars.savedBytes[AVATAR_URL]?.toList())
        }

    @Test
    fun `upload current user avatar saves returned avatar url and caches selected bytes`() =
        runTest {
            val dao = FakeUserDao()
            dao.upsert(UserEntity(id = "user-1", email = "ada@example.com", username = "ada", isCurrentUser = true))
            val remote = FakeUserRemoteDataSource(users = emptyList(), updatedAvatarUrl = AVATAR_URL)
            val avatars = FakeUserAvatarLocalDataSource()
            val repository = UserRepositoryImpl(dao, remote, avatars, NoOpLogger)

            val updatedUser = repository.uploadCurrentUserAvatar(byteArrayOf(9, 8), "avatar.png", "image/png")

            assertEquals(AVATAR_URL, updatedUser?.remoteProfilePictureUrl)
            assertEquals(AVATAR_URL, dao.upsertedUsers.last().remoteProfilePictureUrl)
            assertEquals(byteArrayOf(9, 8).toList(), avatars.savedBytes[AVATAR_URL]?.toList())
            assertEquals(listOf("avatar.png"), remote.uploadedFileNames)
        }
}

private fun avatarUser() =
    User(
        id = "user-1",
        email = "ada@example.com",
        username = "ada",
        remoteProfilePictureUrl = AVATAR_URL,
    )

private const val AVATAR_URL = "http://localhost:8200/api/v1/users/avatars/avatar.png"

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

    override suspend fun getUsersByIds(ids: List<String>): List<UserEntity> = upsertedUsers.filter { user -> user.id in ids }

    override suspend fun getUserByUsername(username: String): UserEntity? = upsertedUsers.firstOrNull { user -> user.username == username }

    override suspend fun updateLocalProfilePicturePath(
        userId: String,
        path: String,
    ) {
        upsertedUsers.indexOfLast { user -> user.id == userId }.takeIf { index -> index >= 0 }?.let { index ->
            upsertedUsers[index] = upsertedUsers[index].copy(localProfilePicturePath = path)
        }
        if (currentUser.value?.id == userId) {
            currentUser.value = currentUser.value?.copy(localProfilePicturePath = path)
        }
    }
}

private class FakeUserRemoteDataSource(
    val users: List<User>,
    private val currentUser: User? = null,
    private val avatarBytes: ByteArray? = null,
    private val updatedAvatarUrl: String? = null,
) : UserRemoteDataSource {
    val queries = mutableListOf<String>()
    val downloadedAvatarFilenames = mutableListOf<String>()
    val uploadedFileNames = mutableListOf<String>()

    override suspend fun searchUsers(query: String): List<User> {
        queries += query
        return users
    }

    override suspend fun getCurrentUser(): User? = currentUser

    override suspend fun downloadAvatar(filename: String): ByteArray? {
        downloadedAvatarFilenames += filename
        return avatarBytes
    }

    override suspend fun updateCurrentUserAvatar(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): String? {
        uploadedFileNames += fileName
        return updatedAvatarUrl
    }
}

private class FakeUserAvatarLocalDataSource(
    private val cachedPaths: MutableMap<String, String> = mutableMapOf(),
) : UserAvatarLocalDataSource {
    val savedBytes = mutableMapOf<String, ByteArray>()

    override fun cachedPathFor(avatarUrl: String): String? = cachedPaths[avatarUrl]

    override fun saveAvatar(
        avatarUrl: String,
        bytes: ByteArray,
    ): String {
        savedBytes[avatarUrl] = bytes
        return "/local/${avatarUrl.hashCode()}.img"
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
