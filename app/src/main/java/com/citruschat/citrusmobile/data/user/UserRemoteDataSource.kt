package com.citruschat.citrusmobile.data.user

import com.citruschat.citrusmobile.domain.model.User

interface UserRemoteDataSource {
    suspend fun searchUsers(query: String): List<User>
}
