package com.citruschat.citrusmobile.data.remote.ws

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StompFrameCodecTest {
    @Test
    fun connectFrame_includesBearerAuthorizationHeaderAndTerminator() {
        val frame = StompFrameCodec.connectFrame("token-123")

        assertEquals(
            stompFrame(
                "CONNECT",
                "accept-version:1.2",
                "heart-beat:10000,10000",
                "Authorization:Bearer token-123",
            ),
            frame,
        )
    }

    @Test
    fun subscribeFrame_targetsChatroomTopicUsedByWebClient() {
        val frame = StompFrameCodec.subscribeFrame("chatroom-7")

        assertEquals(
            stompFrame(
                "SUBSCRIBE",
                "id:chatroom-chatroom-7",
                "destination:/topic/chatrooms/chatroom-7",
                "ack:auto",
            ),
            frame,
        )
    }

    @Test
    fun parseEvents_mapsTopicMessageToChatRoomMessageEvent() {
        val frame =
            stompFrame(
                "MESSAGE",
                "subscription:chatroom-chatroom-7",
                "destination:/topic/chatrooms/chatroom-7",
                body = "{}",
            )

        val events = StompFrameCodec.parseEvents(frame)

        assertEquals(listOf(StompFrameEvent.ChatRoomMessage("chatroom-7")), events)
    }

    @Test
    fun parseEvents_handlesMultipleFramesFromOneSocketMessage() {
        val frame =
            stompFrame("CONNECTED") +
                stompFrame(
                    "MESSAGE",
                    "destination:/topic/chatrooms/chatroom-9",
                    body = "{}",
                )

        val events = StompFrameCodec.parseEvents(frame)

        assertEquals(
            listOf(
                StompFrameEvent.Connected,
                StompFrameEvent.ChatRoomMessage("chatroom-9"),
            ),
            events,
        )
    }

    @Test
    fun disconnectFrame_isValidStompDisconnect() {
        assertTrue(StompFrameCodec.disconnectFrame.startsWith("DISCONNECT\n\n"))
        assertTrue(StompFrameCodec.disconnectFrame.endsWith("\u0000"))
    }

    private fun stompFrame(
        command: String,
        vararg headers: String,
        body: String = "",
    ): String =
        buildString {
            append(command)
            append('\n')
            headers.forEach { header ->
                append(header)
                append('\n')
            }
            append('\n')
            append(body)
            append('\u0000')
        }
}
