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

        override fun connect(
            url: String,
            accessToken: String?,
        ) {
            if (socketRef.get() != null) {
                logger.w(TAG, "Realtime connect ignored because socket already exists")
                return
            }
            logger.i(TAG, "Realtime connect requested for url=$url")

            val requestBuilder = Request.Builder().url(url)
            if (!accessToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $accessToken")
                logger.d(TAG, "Realtime auth header attached")
            }

            val request = requestBuilder.build()

            val webSocket =
                okHttpClient.newWebSocket(
                    request,
                    object : WebSocketListener() {
                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response,
                        ) {
                            logger.i(TAG, "Realtime connected")
                            _events.tryEmit(ChatRealtimeEvent.Connected)
                        }

                        override fun onMessage(
                            webSocket: WebSocket,
                            text: String,
                        ) {
                            logger.v(TAG, "Realtime message received length=${text.length}")
                            _events.tryEmit(ChatRealtimeEvent.TextMessage(text))
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
                            socketRef.set(null)
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?,
                        ) {
                            logger.e(TAG, "Realtime failure", t)
                            _events.tryEmit(ChatRealtimeEvent.Failure(t))
                            socketRef.set(null)
                        }
                    },
                )

            socketRef.set(webSocket)
        }

        override fun disconnect() {
            logger.i(TAG, "Realtime disconnect requested")
            socketRef.getAndSet(null)?.close(SOCKET_CLOSE_NORMAL, "client disconnect")
        }
    }

private const val TAG = "RealtimeClient"
