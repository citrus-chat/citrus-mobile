package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.auth.AuthRemoteDataSource
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.data.device.DeviceIdentityProvider
import com.citruschat.citrusmobile.domain.auth.AuthResult
import com.citruschat.citrusmobile.domain.auth.AuthState
import com.citruschat.citrusmobile.domain.model.User
import com.citruschat.citrusmobile.domain.repository.AuthRepository
import com.citruschat.citrusmobile.domain.repository.UserRepository
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
        private val authRemoteDataSource: AuthRemoteDataSource,
        private val tokenStore: TokenStore,
        private val deviceIdentityProvider: DeviceIdentityProvider,
        private val userRepository: UserRepository,
        private val logger: Logger,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : AuthRepository {
        override suspend fun login(
            username: String,
            password: String,
        ): AuthResult =
            withContext(ioDispatcher) {
                logger.i(TAG, "Auth login started")
                val deviceIdentity = deviceIdentityProvider.getOrCreateDeviceIdentity()

                when (val result = authRemoteDataSource.login(username, password, deviceIdentity)) {
                    is AuthResult.Success -> {
                        tokenStore.saveTokens(result.tokens)
                        userRepository.saveCurrentUser(result.user ?: username.toFallbackUser())
                        logger.i(TAG, "Auth login succeeded")
                        result
                    }
                    is AuthResult.Error -> {
                        logger.w(TAG, "Auth login failed with ${result.error}")
                        result
                    }
                }
            }

        override suspend fun logout() {
            withContext(ioDispatcher) {
                logger.i(TAG, "Auth logout started")
                tokenStore.clearTokens()
                deviceIdentityProvider.clearDeviceIdentity()
                userRepository.clearCurrentUser()
                logger.i(TAG, "Auth logout finished")
            }
        }

        override fun observeAuthState(): Flow<AuthState> =
            tokenStore
                .observeTokens()
                .map { tokens ->
                    if (tokens?.accessToken.isNullOrBlank()) {
                        logger.d(TAG, "Auth state mapped to unauthenticated")
                        AuthState.Unauthenticated
                    } else {
                        logger.d(TAG, "Auth state mapped to authenticated")
                        AuthState.Authenticated
                    }
                }.onStart {
                    logger.v(TAG, "Auth state observation started")
                    emit(AuthState.Loading)
                }

        private fun String.toFallbackUser(): User {
            val email = trim()
            return User(
                id = email,
                email = email,
                username = email.substringBefore('@'),
            )
        }
    }

private const val TAG = "AuthRepository"
