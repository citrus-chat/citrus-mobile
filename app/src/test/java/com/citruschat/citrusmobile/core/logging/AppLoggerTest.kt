package com.citruschat.citrusmobile.core.logging

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLoggerTest {
    @Test
    fun `does not log when disabled`() {
        val sink = FakeLogSink()
        val logger = AppLogger(isEnabled = false, logSink = sink)

        logger.i(tag = "Test", message = "hello")

        assertEquals(0, sink.entries.size)
    }

    @Test
    fun `logs when enabled`() {
        val sink = FakeLogSink()
        val logger = AppLogger(isEnabled = true, logSink = sink)

        logger.e(tag = "Test", message = "boom")

        assertEquals(1, sink.entries.size)
        assertEquals(LogLevel.ERROR, sink.entries.first().level)
        assertEquals("Test", sink.entries.first().tag)
        assertEquals("boom", sink.entries.first().message)
    }

    @Test
    fun `logs all levels when enabled`() {
        val sink = FakeLogSink()
        val logger = AppLogger(isEnabled = true, logSink = sink)

        logger.v(tag = "VerboseTag", message = "v")
        logger.d(tag = "DebugTag", message = "d")
        logger.i(tag = "InfoTag", message = "i")
        logger.w(tag = "WarnTag", message = "w")

        assertEquals(4, sink.entries.size)
        assertEquals(LogLevel.VERBOSE, sink.entries[0].level)
        assertEquals(LogLevel.DEBUG, sink.entries[1].level)
        assertEquals(LogLevel.INFO, sink.entries[2].level)
        assertEquals(LogLevel.WARN, sink.entries[3].level)
    }
}

private class FakeLogSink : LogSink {
    val entries = mutableListOf<Entry>()

    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        entries += Entry(level, tag, message, throwable)
    }

    data class Entry(
        val level: LogLevel,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
    )
}
