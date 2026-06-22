package com.citruschat.citrusmobile.data.user

import com.citruschat.citrusmobile.domain.model.User

interface UserRemoteDataSource {
    suspend fun searchUsers(query: String): List<User>

    suspend fun getCurrentUser(): User?

    suspend fun downloadAvatar(filename: String): ByteArray?

    suspend fun updateCurrentUserAvatar(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): String?
}
