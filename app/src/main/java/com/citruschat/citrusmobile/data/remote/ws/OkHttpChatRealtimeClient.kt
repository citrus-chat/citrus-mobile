package com.citruschat.citrusmobile.data.remote.ws

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.domain.realtime.ChatRealtimeClient
import com.citruschat.citrusmobile.domain.realtime.ChatRealtimeEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

const val SOCKET_CLOSE_NORMAL = 1000

@Singleton
class OkHttpChatRealtimeClient
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val logger: Logger,
    ) : ChatRealtimeClient {
        private val _events =
            MutableSharedFlow<ChatRealtimeEvent>(
                replay = 0,
                extraBufferCapacity = 64,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        override val events: Flow<ChatRealtimeEvent> = _events

        private val socketRef = AtomicReference<WebSocket?>(null)
        private val stompConnected = AtomicBoolean(false)
        private val subscribedChatRooms = ConcurrentHashMap.newKeySet<String>()
        private val accessTokenRef = AtomicReference<String?>()

        override fun connect(
            url: String,
            accessToken: String?,
        ) {
            accessTokenRef.set(accessToken)
            if (socketRef.get() != null) {
                logger.w(TAG, "Realtime connect ignored because socket already exists")
                return
            }
            logger.i(TAG, "Realtime connect requested for url=$url")

            val request = Request.Builder().url(url).build()

            val webSocket =
                okHttpClient.newWebSocket(
                    request,
                    object : WebSocketListener() {
                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response,
                        ) {
                            logger.i(TAG, "Realtime socket opened")
                            webSocket.send(StompFrameCodec.connectFrame(accessTokenRef.get()))
                        }

                        override fun onMessage(
                            webSocket: WebSocket,
                            text: String,
                        ) {
                            handleFrame(webSocket, text)
                        }

                        override fun onClosing(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String,
                        ) {
                            logger.w(TAG, "Realtime closing: $code $reason")
                            _events.tryEmit(ChatRealtimeEvent.Disconnected("closing: $code $reason"))
                            webSocket.close(code, reason)
                        }

                        override fun onClosed(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String,
                        ) {
                            logger.i(TAG, "Realtime closed: $code $reason")
                            _events.tryEmit(ChatRealtimeEvent.Disconnected("closed: $code $reason"))
                            stompConnected.set(false)
                            socketRef.set(null)
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?,
                        ) {
                            logger.e(TAG, "Realtime failure", t)
                            _events.tryEmit(ChatRealtimeEvent.Failure(t))
                            stompConnected.set(false)
                            socketRef.set(null)
                        }
                    },
                )

            socketRef.set(webSocket)
        }

        override fun subscribeToChatRoom(chatRoomId: String) {
            if (chatRoomId.isBlank()) return
            subscribedChatRooms.add(chatRoomId)
            if (stompConnected.get()) {
                socketRef.get()?.send(StompFrameCodec.subscribeFrame(chatRoomId))
            }
        }

        override fun unsubscribeFromChatRoom(chatRoomId: String) {
            if (chatRoomId.isBlank()) return
            subscribedChatRooms.remove(chatRoomId)
            socketRef.get()?.send(StompFrameCodec.unsubscribeFrame(chatRoomId))
        }

        override fun disconnect() {
            logger.i(TAG, "Realtime disconnect requested")
            socketRef.get()?.send(StompFrameCodec.disconnectFrame)
            socketRef.getAndSet(null)?.close(SOCKET_CLOSE_NORMAL, "client disconnect")
            stompConnected.set(false)
            subscribedChatRooms.clear()
        }

        private fun handleFrame(
            webSocket: WebSocket,
            rawFrame: String,
        ) {
            StompFrameCodec.parseEvents(rawFrame).forEach { event ->
                when (event) {
                    StompFrameEvent.Connected -> {
                        stompConnected.set(true)
                        subscribedChatRooms.forEach { chatRoomId ->
                            webSocket.send(StompFrameCodec.subscribeFrame(chatRoomId))
                        }
                        logger.i(TAG, "Realtime STOMP connected")
                        _events.tryEmit(ChatRealtimeEvent.Connected)
                    }
                    is StompFrameEvent.ChatRoomMessage -> {
                        _events.tryEmit(ChatRealtimeEvent.ChatRoomMessage(event.chatRoomId))
                    }
                    is StompFrameEvent.TextMessage -> {
                        _events.tryEmit(ChatRealtimeEvent.TextMessage(event.frame))
                    }
                    StompFrameEvent.Error -> {
                        logger.w(TAG, "Realtime STOMP error frame received")
                        _events.tryEmit(ChatRealtimeEvent.Disconnected("stomp error"))
                    }
                    is StompFrameEvent.Ignored -> {
                        logger.v(TAG, "Realtime STOMP frame ignored command=${event.command}")
                    }
                }
            }
        }
    }

private const val TAG = "RealtimeClient"
