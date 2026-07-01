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
import java.util.concurrent.atomic.AtomicInteger
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
        private val subscriptions = ConcurrentHashMap<String, String>()
        private val subscriptionCounter = AtomicInteger(1)
        private val connected = AtomicReference(false)
        private val tokenRef = AtomicReference<String?>(null)

        override val isConnected: Boolean
            get() = connected.get()

        override fun connect(
            url: String,
            accessToken: String?,
        ) {
            if (socketRef.get() != null) {
                if (connected.get()) {
                    logger.v(TAG, "Realtime connect ignored because STOMP is already connected")
                    return
                }
                logger.w(TAG, "Realtime closing stale socket before reconnect")
                socketRef.getAndSet(null)?.close(SOCKET_CLOSE_NORMAL, "client reconnect")
            }
            tokenRef.set(accessToken?.takeIf { it.isNotBlank() })
            connected.set(false)
            logger.i(TAG, "Realtime STOMP connect requested for url=$url")

            val request = Request.Builder().url(url).build()
            val webSocket =
                okHttpClient.newWebSocket(
                    request,
                    object : WebSocketListener() {
                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response,
                        ) {
                            logger.i(TAG, "Realtime socket opened; sending STOMP CONNECT")
                            webSocket.send(connectFrame(tokenRef.get()))
                        }

                        override fun onMessage(
                            webSocket: WebSocket,
                            text: String,
                        ) {
                            handleFrame(text)
                        }

                        override fun onClosing(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String,
                        ) {
                            logger.w(TAG, "Realtime closing: $code $reason")
                            connected.set(false)
                            _events.tryEmit(ChatRealtimeEvent.Disconnected("closing: $code $reason"))
                            webSocket.close(code, reason)
                        }

                        override fun onClosed(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String,
                        ) {
                            logger.i(TAG, "Realtime closed: $code $reason")
                            connected.set(false)
                            _events.tryEmit(ChatRealtimeEvent.Disconnected("closed: $code $reason"))
                            socketRef.set(null)
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?,
                        ) {
                            logger.e(TAG, "Realtime failure", t)
                            connected.set(false)
                            _events.tryEmit(ChatRealtimeEvent.Failure(t))
                            socketRef.set(null)
                        }
                    },
                )

            socketRef.set(webSocket)
        }

        override fun subscribe(destination: String) {
            val cleanDestination = destination.takeIf { it.isNotBlank() } ?: return
            subscriptions.computeIfAbsent(cleanDestination) {
                "sub-${subscriptionCounter.getAndIncrement()}"
            }
            if (connected.get()) {
                sendSubscribeFrame(cleanDestination)
            }
        }

        override fun send(
            destination: String,
            body: String,
        ): Boolean {
            if (!connected.get()) {
                logger.w(TAG, "Realtime SEND ignored because STOMP is not connected")
                return false
            }
            val frame =
                StompCodec.buildFrame(
                    command = "SEND",
                    headers =
                        linkedMapOf(
                            "destination" to destination,
                            "content-type" to "application/json",
                        ),
                    body = body,
                )
            return socketRef.get()?.send(frame) == true
        }

        override fun disconnect() {
            logger.i(TAG, "Realtime disconnect requested")
            socketRef.get()?.send(StompCodec.buildFrame("DISCONNECT", linkedMapOf(), ""))
            subscriptions.clear()
            connected.set(false)
            socketRef.getAndSet(null)?.close(SOCKET_CLOSE_NORMAL, "client disconnect")
        }

        private fun handleFrame(text: String) {
            StompCodec.parseFrames(text).forEach { frame ->
                when (frame.command) {
                    "CONNECTED" -> {
                        connected.set(true)
                        logger.i(TAG, "Realtime STOMP connected")
                        _events.tryEmit(ChatRealtimeEvent.Connected)
                        subscriptions.keys.forEach(::sendSubscribeFrame)
                    }
                    "MESSAGE" -> {
                        val destination = frame.headers["destination"]
                        logger.v(TAG, "Realtime STOMP message received destination=$destination length=${frame.body.length}")
                        _events.tryEmit(ChatRealtimeEvent.TextMessage(destination, frame.body))
                    }
                    "RECEIPT" -> logger.d(TAG, "Realtime STOMP receipt received")
                    "ERROR" -> {
                        logger.w(TAG, "Realtime STOMP error body=${frame.body}")
                        _events.tryEmit(ChatRealtimeEvent.Disconnected(frame.body.ifBlank { "STOMP ERROR" }))
                    }
                    else -> logger.v(TAG, "Realtime STOMP frame ignored command=${frame.command}")
                }
            }
        }

        private fun sendSubscribeFrame(destination: String) {
            val id = subscriptions[destination] ?: return
            val sent =
                socketRef.get()?.send(
                    StompCodec.buildFrame(
                        command = "SUBSCRIBE",
                        headers =
                            linkedMapOf(
                                "id" to id,
                                "destination" to destination,
                            ),
                        body = "",
                    ),
                ) == true
            if (sent) {
                logger.i(TAG, "Realtime subscribed destination=$destination")
                _events.tryEmit(ChatRealtimeEvent.Subscribed(destination))
            }
        }

        private fun connectFrame(accessToken: String?): String {
            val headers =
                linkedMapOf(
                    "accept-version" to "1.2",
                    "heart-beat" to "10000,10000",
                )
            if (!accessToken.isNullOrBlank()) {
                headers["Authorization"] = "Bearer $accessToken"
            }
            return StompCodec.buildFrame("CONNECT", headers, "")
        }
    }

private const val TAG = "RealtimeClient"
