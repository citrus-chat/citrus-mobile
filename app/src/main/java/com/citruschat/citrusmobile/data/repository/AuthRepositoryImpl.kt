package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.data.auth.AuthApiClient
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.domain.model.AuthState
import com.citruschat.citrusmobile.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val authApiClient: AuthApiClient,
        private val tokenStore: TokenStore,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : AuthRepository {
        override suspend fun login(
            username: String,
            password: String,
        ): Result<Unit> =
            runCatching {
                withContext(ioDispatcher) {
                    val tokens = authApiClient.login(username = username, password = password)
                    tokenStore.saveTokens(tokens)
                }
            }

        override suspend fun logout() {
            withContext(ioDispatcher) {
                tokenStore.clearTokens()
            }
        }

        override fun observeAuthState(): Flow<AuthState> =
            tokenStore
                .observeTokens()
                .map { tokens ->
                    if (tokens?.accessToken.isNullOrBlank()) {
                        AuthState.Unauthenticated
                    } else {
                        AuthState.Authenticated
                    }
                }.onStart { emit(AuthState.Loading) }
    }
