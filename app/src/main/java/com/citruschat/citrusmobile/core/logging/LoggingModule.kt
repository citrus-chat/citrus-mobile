package com.citruschat.citrusmobile.core.logging

import com.citruschat.citrusmobile.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class LoggingEnabled

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {
    @Provides
    @Singleton
    @LoggingEnabled
    fun provideLoggingEnabled(): Boolean = BuildConfig.DEBUG && BuildConfig.ENABLE_APP_LOGGING

    @Provides
    @Singleton
    fun provideLogSink(): LogSink = AndroidLogSink

    @Provides
    @Singleton
    fun provideLogger(
        @LoggingEnabled isEnabled: Boolean,
        logSink: LogSink,
    ): Logger = AppLogger(isEnabled = isEnabled, logSink = logSink)
}
