package com.citruschat.citrusmobile.data.remote.ws

internal object StompFrameCodec {
    const val FRAME_TERMINATOR = '\u0000'

    fun connectFrame(accessToken: String?): String =
        buildString {
            appendLine(CONNECT_COMMAND)
            appendLine("accept-version:1.2")
            appendLine("heart-beat:10000,10000")
            accessToken?.takeIf { it.isNotBlank() }?.let { token ->
                appendLine("Authorization:Bearer $token")
            }
            appendLine()
            append(FRAME_TERMINATOR)
        }

    fun subscribeFrame(chatRoomId: String): String =
        buildString {
            appendLine(SUBSCRIBE_COMMAND)
            appendLine("id:${subscriptionId(chatRoomId)}")
            appendLine("destination:$CHATROOM_TOPIC_PREFIX$chatRoomId")
            appendLine("ack:auto")
            appendLine()
            append(FRAME_TERMINATOR)
        }

    fun unsubscribeFrame(chatRoomId: String): String =
        buildString {
            appendLine(UNSUBSCRIBE_COMMAND)
            appendLine("id:${subscriptionId(chatRoomId)}")
            appendLine()
            append(FRAME_TERMINATOR)
        }

    val disconnectFrame: String =
        buildString {
            appendLine(DISCONNECT_COMMAND)
            appendLine()
            append(FRAME_TERMINATOR)
        }

    fun parseEvents(rawFrame: String): List<StompFrameEvent> =
        rawFrame
            .split(FRAME_TERMINATOR)
            .asSequence()
            .map { it.trimEnd('\n', '\r') }
            .filter { it.isNotBlank() }
            .map { frame -> frame.toEvent() }
            .toList()

    private fun String.toEvent(): StompFrameEvent {
        val command = lineSequence().firstOrNull().orEmpty()
        return when (command) {
            CONNECTED_COMMAND -> StompFrameEvent.Connected
            MESSAGE_COMMAND -> {
                val destination = headerValue(DESTINATION_HEADER)
                val chatRoomId = destination?.substringAfter(CHATROOM_TOPIC_PREFIX, missingDelimiterValue = "")
                if (chatRoomId.isNullOrBlank()) {
                    StompFrameEvent.TextMessage(this)
                } else {
                    StompFrameEvent.ChatRoomMessage(chatRoomId)
                }
            }
            ERROR_COMMAND -> StompFrameEvent.Error
            else -> StompFrameEvent.Ignored(command)
        }
    }

    private fun String.headerValue(name: String): String? =
        lineSequence()
            .drop(1)
            .takeWhile { it.isNotBlank() }
            .firstOrNull { it.startsWith("$name:") }
            ?.substringAfter(':')

    private fun subscriptionId(chatRoomId: String): String = "chatroom-$chatRoomId"

    private const val CONNECT_COMMAND = "CONNECT"
    private const val CONNECTED_COMMAND = "CONNECTED"
    private const val SUBSCRIBE_COMMAND = "SUBSCRIBE"
    private const val UNSUBSCRIBE_COMMAND = "UNSUBSCRIBE"
    private const val DISCONNECT_COMMAND = "DISCONNECT"
    private const val MESSAGE_COMMAND = "MESSAGE"
    private const val ERROR_COMMAND = "ERROR"
    private const val DESTINATION_HEADER = "destination"
    private const val CHATROOM_TOPIC_PREFIX = "/topic/chatrooms/"
}

internal sealed interface StompFrameEvent {
    data object Connected : StompFrameEvent

    data class ChatRoomMessage(
        val chatRoomId: String,
    ) : StompFrameEvent

    data class TextMessage(
        val frame: String,
    ) : StompFrameEvent

    data object Error : StompFrameEvent

    data class Ignored(
        val command: String,
    ) : StompFrameEvent
}
