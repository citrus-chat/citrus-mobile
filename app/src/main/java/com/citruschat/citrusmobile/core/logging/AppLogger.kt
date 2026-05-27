package com.citruschat.citrusmobile.core.logging

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLogger
    @Inject
    constructor(
        @LoggingEnabled private val isEnabled: Boolean,
        private val logSink: LogSink,
    ) : Logger {
        override fun v(
            tag: String,
            message: String,
        ) {
            log(LogLevel.VERBOSE, tag, message)
        }

        override fun d(
            tag: String,
            message: String,
        ) {
            log(LogLevel.DEBUG, tag, message)
        }

        override fun i(
            tag: String,
            message: String,
        ) {
            log(LogLevel.INFO, tag, message)
        }

        override fun w(
            tag: String,
            message: String,
            throwable: Throwable?,
        ) {
            log(LogLevel.WARN, tag, message, throwable)
        }

        override fun e(
            tag: String,
            message: String,
            throwable: Throwable?,
        ) {
            log(LogLevel.ERROR, tag, message, throwable)
        }

        private fun log(
            level: LogLevel,
            tag: String,
            message: String,
            throwable: Throwable? = null,
        ) {
            if (!isEnabled) return
            logSink.log(level = level, tag = tag, message = message, throwable = throwable)
        }
    }
