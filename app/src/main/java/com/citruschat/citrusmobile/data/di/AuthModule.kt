package com.citruschat.citrusmobile.data.di

import com.citruschat.citrusmobile.BuildConfig
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.auth.AuthApiClient
import com.citruschat.citrusmobile.data.auth.AuthRemoteDataSource
import com.citruschat.citrusmobile.data.auth.EncryptedPrefsTokenStore
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.data.device.DeviceIdentityProvider
import com.citruschat.citrusmobile.data.device.EncryptedPrefsDeviceIdentityProvider
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
    fun provideAuthRemoteDataSource(authApiClient: AuthApiClient): AuthRemoteDataSource = authApiClient

    @Provides
    @Singleton
    fun provideTokenStore(tokenStore: EncryptedPrefsTokenStore): TokenStore = tokenStore

    @Provides
    @Singleton
    fun provideDeviceIdentityProvider(
        provider: EncryptedPrefsDeviceIdentityProvider,
    ): DeviceIdentityProvider = provider

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
        authRemoteDataSource: AuthRemoteDataSource,
        tokenStore: TokenStore,
        deviceIdentityProvider: DeviceIdentityProvider,
        userRepository: UserRepository,
        logger: Logger,
    ): AuthRepository =
        AuthRepositoryImpl(
            authRemoteDataSource = authRemoteDataSource,
            tokenStore = tokenStore,
            deviceIdentityProvider = deviceIdentityProvider,
            userRepository = userRepository,
            logger = logger,
        )
}
