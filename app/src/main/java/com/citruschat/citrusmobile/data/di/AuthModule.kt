package com.citruschat.citrusmobile.data.di

import com.citruschat.citrusmobile.BuildConfig
import com.citruschat.citrusmobile.data.auth.AuthApiClient
import com.citruschat.citrusmobile.data.auth.EncryptedPrefsTokenStore
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.data.repository.AuthRepositoryImpl
import com.citruschat.citrusmobile.domain.repository.AuthRepository
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
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideAuthApiClient(okHttpClient: OkHttpClient): AuthApiClient =
        AuthApiClient(
            okHttpClient = okHttpClient,
            baseUrl = BuildConfig.API_BASE_URL,
        )

    @Provides
    @Singleton
    fun provideTokenStore(tokenStore: EncryptedPrefsTokenStore): TokenStore = tokenStore

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiClient: AuthApiClient,
        tokenStore: TokenStore,
    ): AuthRepository = AuthRepositoryImpl(authApiClient = authApiClient, tokenStore = tokenStore)
}
