package com.citruschat.citrusmobile.domain.realtime

import kotlinx.coroutines.flow.Flow

interface ChatRealtimeClient {
    val events: Flow<ChatRealtimeEvent>

    val isConnected: Boolean

    fun connect(
        url: String,
        accessToken: String?,
    )

    fun subscribe(destination: String)

    fun send(
        destination: String,
        body: String,
    ): Boolean

    fun disconnect()
}

sealed interface ChatRealtimeEvent {
    data object Connected : ChatRealtimeEvent

    data class Subscribed(
        val destination: String,
    ) : ChatRealtimeEvent

    data class Disconnected(
        val reason: String? = null,
    ) : ChatRealtimeEvent

    data class Failure(
        val throwable: Throwable,
    ) : ChatRealtimeEvent

    data class TextMessage(
        val destination: String?,
        val text: String,
    ) : ChatRealtimeEvent
}
