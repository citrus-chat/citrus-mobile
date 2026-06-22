package com.citruschat.citrusmobile.domain.repository

import com.citruschat.citrusmobile.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeCurrentUser(): Flow<User?>

    suspend fun searchUsers(query: String): List<User>

    suspend fun refreshCurrentUser(): User?

    suspend fun getAvatarLocalPath(user: User): String?

    suspend fun getAvatarLocalPath(avatarUrl: String?): String?

    suspend fun uploadCurrentUserAvatar(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): User?

    suspend fun saveCurrentUser(user: User)

    suspend fun saveUsers(users: List<User>)

    suspend fun clearCurrentUser()
}
