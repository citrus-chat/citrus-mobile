package com.citruschat.citrusmobile.data.di

import com.citruschat.citrusmobile.BuildConfig
import com.citruschat.citrusmobile.core.logging.Logger
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
    fun provideOkHttpClient(logger: Logger): OkHttpClient {
        val logging =
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.ENABLE_APP_LOGGING) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }
        logger.i(TAG, "Creating OkHttpClient")

        return OkHttpClient
            .Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS) // important for long-lived connections
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                logger.d(TAG, "HTTP ${request.method} ${request.url} started")
                runCatching { chain.proceed(request) }
                    .onSuccess { response ->
                        logger.i(TAG, "HTTP ${request.method} ${request.url} -> ${response.code}")
                    }.onFailure { throwable ->
                        logger.e(TAG, "HTTP ${request.method} ${request.url} failed", throwable)
                    }.getOrThrow()
            }
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

private const val TAG = "NetworkModule"
