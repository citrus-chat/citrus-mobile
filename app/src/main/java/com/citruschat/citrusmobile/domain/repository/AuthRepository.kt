package com.citruschat.citrusmobile.domain.repository

import com.citruschat.citrusmobile.domain.auth.AuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(
        username: String,
        password: String,
    ): Result<Unit>

    suspend fun logout()

    fun observeAuthState(): Flow<AuthState>
}
