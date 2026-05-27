package com.citruschat.citrusmobile.domain.realtime

import kotlinx.coroutines.flow.Flow

interface ChatRealtimeClient {
    /** Stream of realtime events coming from the server. */
    val events: Flow<ChatRealtimeEvent>

    /** Connect (idempotent). Prefer calling from Repository. */
    fun connect(
        url: String,
        accessToken: String?,
    )

    /** Disconnect (idempotent). */
    fun disconnect()
}

sealed interface ChatRealtimeEvent {
    data object Connected : ChatRealtimeEvent

    data class Disconnected(
        val reason: String? = null,
    ) : ChatRealtimeEvent

    data class Failure(
        val throwable: Throwable,
    ) : ChatRealtimeEvent

    /** Raw payload for now; later decode to typed events (new message, etc.). */
    data class TextMessage(
        val text: String,
    ) : ChatRealtimeEvent
}
