package com.citruschat.citrusmobile.data.di

import com.citruschat.citrusmobile.BuildConfig
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.auth.AuthApiClient
import com.citruschat.citrusmobile.data.auth.EncryptedPrefsTokenStore
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.data.repository.AuthRepositoryImpl
import com.citruschat.citrusmobile.data.user.UserApiClient
import com.citruschat.citrusmobile.data.user.UserRemoteDataSource
import com.citruschat.citrusmobile.domain.repository.AuthRepository
import com.citruschat.citrusmobile.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideAuthApiClient(
        okHttpClient: OkHttpClient,
        logger: Logger,
    ): AuthApiClient =
        AuthApiClient(
            okHttpClient = okHttpClient,
            logger = logger,
            baseUrl = BuildConfig.API_BASE_URL,
        )

    @Provides
    @Singleton
    fun provideTokenStore(tokenStore: EncryptedPrefsTokenStore): TokenStore = tokenStore

    @Provides
    @Singleton
    fun provideUserApiClient(
        okHttpClient: OkHttpClient,
        tokenStore: TokenStore,
        logger: Logger,
    ): UserApiClient =
        UserApiClient(
            okHttpClient = okHttpClient,
            tokenStore = tokenStore,
            logger = logger,
            baseUrl = BuildConfig.API_BASE_URL,
        )

    @Provides
    @Singleton
    fun provideUserRemoteDataSource(userApiClient: UserApiClient): UserRemoteDataSource = userApiClient

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiClient: AuthApiClient,
        tokenStore: TokenStore,
        userRepository: UserRepository,
        logger: Logger,
    ): AuthRepository =
        AuthRepositoryImpl(
            authApiClient = authApiClient,
            tokenStore = tokenStore,
            userRepository = userRepository,
            logger = logger,
        )
}
