package com.citruschat.citrusmobile.data.remote.ws

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StompCodecTest {
    @Test
    fun `buildFrame serializes command headers body and null terminator`() {
        val frame =
            StompCodec.buildFrame(
                command = "SEND",
                headers =
                    linkedMapOf(
                        "destination" to "/app/chat/sendMessage",
                        "content-type" to "application/json",
                    ),
                body = """{"messageId":"message-1"}""",
            )

        assertEquals(
            "SEND\n" +
                "destination:/app/chat/sendMessage\n" +
                "content-type:application/json\n" +
                "\n" +
                """{"messageId":"message-1"}""" +
                "\u0000",
            frame,
        )
    }

    @Test
    fun `parseFrames parses message destination and body`() {
        val frames =
            StompCodec.parseFrames(
                "MESSAGE\n" +
                    "destination:/topic/chatrooms/chat-1\n" +
                    "subscription:sub-1\n" +
                    "\n" +
                    """{"id":"message-1"}""" +
                    "\u0000",
            )

        assertEquals(1, frames.size)
        assertEquals("MESSAGE", frames.single().command)
        assertEquals("/topic/chatrooms/chat-1", frames.single().headers["destination"])
        assertEquals("""{"id":"message-1"}""", frames.single().body)
    }

    @Test
    fun `parseFrames parses multiple frames from one websocket message`() {
        val frames =
            StompCodec.parseFrames(
                "CONNECTED\nversion:1.2\n\n\u0000" +
                    "MESSAGE\ndestination:/topic/chatrooms/chat-1\n\npayload\u0000",
            )

        assertEquals(listOf("CONNECTED", "MESSAGE"), frames.map { frame -> frame.command })
        assertEquals("payload", frames[1].body)
    }

    @Test
    fun `parseFrames ignores blank heartbeat frames`() {
        val frames = StompCodec.parseFrames("\n\u0000\r\n\u0000")

        assertTrue(frames.isEmpty())
    }
}
