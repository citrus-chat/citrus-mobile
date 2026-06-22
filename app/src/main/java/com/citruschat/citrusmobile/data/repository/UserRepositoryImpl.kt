package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.local.dao.UserDao
import com.citruschat.citrusmobile.data.mapper.toDomain
import com.citruschat.citrusmobile.data.mapper.toEntity
import com.citruschat.citrusmobile.data.user.UserAvatarLocalDataSource
import com.citruschat.citrusmobile.data.user.UserRemoteDataSource
import com.citruschat.citrusmobile.domain.model.User
import com.citruschat.citrusmobile.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

class UserRepositoryImpl
    @Inject
    constructor(
        private val dao: UserDao,
        private val userRemoteDataSource: UserRemoteDataSource,
        private val avatarLocalDataSource: UserAvatarLocalDataSource,
        private val logger: Logger,
    ) : UserRepository {
        override fun observeCurrentUser(): Flow<User?> =
            dao
                .observeCurrentUser()
                .map { entity -> entity?.toDomain() }

        override suspend fun searchUsers(query: String): List<User> {
            if (query.isBlank()) return emptyList()
            logger.d(TAG, "Searching users query=$query")
            val users = userRemoteDataSource.searchUsers(query.trim())
            return saveAndReturnUsers(users)
        }

        override suspend fun refreshCurrentUser(): User? {
            val remoteUser = userRemoteDataSource.getCurrentUser() ?: return null
            val currentUser = observeCurrentUser().firstOrNull()
            val mergedUser =
                remoteUser.copy(
                    localProfilePicturePath = remoteUser.resolveLocalProfilePicturePath(currentUser),
                    statusMessage = currentUser?.statusMessage,
                    createdAt = currentUser?.createdAt.orEmpty(),
                    isCurrentUser = true,
                )
            saveCurrentUser(mergedUser)
            return mergedUser
        }

        override suspend fun getAvatarLocalPath(user: User): String? {
            user.localProfilePicturePath
                ?.takeIf { path -> File(path).exists() && File(path).length() > 0L }
                ?.let { path -> return path }

            val remoteUrl = user.remoteProfilePictureUrl ?: return null

            avatarLocalDataSource.cachedPathFor(remoteUrl)?.let { path ->
                dao.updateLocalProfilePicturePath(user.id, path)
                return path
            }

            val filename = remoteUrl.toAvatarFilename() ?: return null
            val bytes = userRemoteDataSource.downloadAvatar(filename) ?: return null

            val savedPath = avatarLocalDataSource.saveAvatar(remoteUrl, bytes) ?: return null
            dao.updateLocalProfilePicturePath(user.id, savedPath)

            return savedPath
        }

        override suspend fun getAvatarLocalPath(avatarUrl: String?): String? {
            val remoteAvatarUrl = avatarUrl?.takeIf { it.isNotBlank() } ?: return null

            avatarLocalDataSource.cachedPathFor(remoteAvatarUrl)?.let { path ->
                return path
            }

            val filename = remoteAvatarUrl.toAvatarFilename() ?: return null
            val bytes = userRemoteDataSource.downloadAvatar(filename) ?: return null

            return avatarLocalDataSource.saveAvatar(remoteAvatarUrl, bytes)
        }

        override suspend fun uploadCurrentUserAvatar(
            bytes: ByteArray,
            fileName: String,
            mimeType: String,
        ): User? {
            val remoteAvatarUrl =
                userRemoteDataSource.updateCurrentUserAvatar(bytes, fileName, mimeType)
                    ?: return null

            val localAvatarPath =
                avatarLocalDataSource.saveAvatar(remoteAvatarUrl, bytes)
                    ?: return null

            val currentUser =
                observeCurrentUser().firstOrNull()
                    ?: refreshCurrentUser()
                    ?: return null

            val updatedUser =
                currentUser.copy(
                    localProfilePicturePath = localAvatarPath,
                    remoteProfilePictureUrl = remoteAvatarUrl,
                    isCurrentUser = true,
                )

            saveCurrentUser(updatedUser)
            return updatedUser
        }

        override suspend fun saveCurrentUser(user: User) {
            logger.i(TAG, "Saving current user id=${user.id}")
            val existingUser = dao.getUsersByIds(listOf(user.id)).firstOrNull()?.toDomain()
            val userWithAvatar =
                user.copy(
                    localProfilePicturePath = user.resolveLocalProfilePicturePath(existingUser),
                    isCurrentUser = true,
                )
            dao.clearCurrentUserFlag()
            dao.upsert(userWithAvatar.toEntity())
        }

        override suspend fun saveUsers(users: List<User>) {
            saveAndReturnUsers(users)
        }

        private suspend fun saveAndReturnUsers(users: List<User>): List<User> {
            if (users.isEmpty()) return emptyList()

            logger.d(TAG, "Saving users count=${users.size}")

            val existingUsers =
                dao
                    .getUsersByIds(users.map { it.id })
                    .associateBy { it.id }

            val mergedUsers =
                users.map { remoteUser ->
                    val existingUser = existingUsers[remoteUser.id]?.toDomain()

                    remoteUser.copy(
                        localProfilePicturePath = remoteUser.resolveLocalProfilePicturePath(existingUser),
                        isCurrentUser = existingUser?.isCurrentUser ?: remoteUser.isCurrentUser,
                    )
                }

            dao.upsertAll(mergedUsers.map { it.toEntity() })
            return mergedUsers
        }

        override suspend fun clearCurrentUser() {
            logger.i(TAG, "Clearing current user flag")
            dao.clearCurrentUserFlag()
        }

        private suspend fun User.resolveLocalProfilePicturePath(existingUser: User?): String? {
            val remoteUrl = remoteProfilePictureUrl?.takeIf { it.isNotBlank() } ?: return null
            val existingPath =
                existingUser
                    ?.localProfilePicturePath
                    ?.takeIf { existingUser.remoteProfilePictureUrl == remoteUrl }
                    ?.takeIf(::isValidLocalAvatarPath)
            if (existingPath != null) return existingPath

            avatarLocalDataSource.cachedPathFor(remoteUrl)?.let { path -> return path }

            val filename = remoteUrl.toAvatarFilename() ?: return null
            val bytes = userRemoteDataSource.downloadAvatar(filename) ?: return null
            return avatarLocalDataSource.saveAvatar(remoteUrl, bytes)
        }

        private fun isValidLocalAvatarPath(path: String): Boolean {
            val file = File(path)
            return file.exists() &&
                file.length() > 0L
        }

        private fun String.toAvatarFilename(): String? =
            substringBefore('?')
                .trimEnd('/')
                .substringAfterLast('/')
                .takeIf { it.isNotBlank() }
    }

private const val TAG = "UserRepository"
