package com.citruschat.citrusmobile.data.auth

import kotlinx.coroutines.flow.Flow

interface TokenStore {
    fun observeTokens(): Flow<AuthTokens?>

    suspend fun saveTokens(tokens: AuthTokens)

    suspend fun clearTokens()
}
