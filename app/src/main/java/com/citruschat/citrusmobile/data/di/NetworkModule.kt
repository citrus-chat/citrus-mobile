package com.citruschat.citrusmobile.data.di

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.core.logging.LoggingEnabled
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
const val READ_TIMEOUT = 0L // no timeout for long-lived connections
const val WRITE_TIMEOUT = 10L

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        logger: Logger,
        @LoggingEnabled isLoggingEnabled: Boolean,
    ): OkHttpClient {
        val logging =
            HttpLoggingInterceptor { message ->
                logger.d(TAG, "HTTP $message")
            }.apply {
                level = if (isLoggingEnabled) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }
        val builder =
            OkHttpClient
                .Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS) // 0 means no timeout
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(logging)

        if (isLoggingEnabled) {
            logger.i(TAG, "Creating OkHttpClient with request logging enabled")
        }

        return builder.build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RealtimeModule {
    @Binds
    @Singleton
    abstract fun bindChatRealtimeClient(impl: OkHttpChatRealtimeClient): ChatRealtimeClient
}

private const val TAG = "NetworkModule"
