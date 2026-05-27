package com.citruschat.citrusmobile.data.di

import com.citruschat.citrusmobile.data.remote.ws.OkHttpChatRealtimeClient
import com.citruschat.citrusmobile.domain.realtime.ChatRealtimeClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

const val CONNECT_TIMEOUT = 10L
const val READ_TIMEOUT = 0L // important for long-lived connections
const val WRITE_TIMEOUT = 10L

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

        return OkHttpClient
            .Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS) // important for long-lived connections
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RealtimeModule {
    @Binds
    @Singleton
    abstract fun bindChatRealtimeClient(impl: OkHttpChatRealtimeClient): ChatRealtimeClient
}
