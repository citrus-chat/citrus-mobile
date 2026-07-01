package com.citruschat.citrusmobile.data.remote.ws

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.domain.realtime.ChatRealtimeEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class OkHttpChatRealtimeClientTest {
    private lateinit var server: MockWebServer
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        okHttpClient = OkHttpClient()
    }

    @After
    fun tearDown() {
        okHttpClient.connectionPool.evictAll()
        okHttpClient.dispatcher.executorService.shutdown()
        try {
            server.shutdown()
        } catch (_: IOException) {
            // MockWebServer can time out while closing an upgraded websocket; the test closes the client explicitly.
        }
    }

    @Test
    fun `connect subscribes sends and emits topic message`() =
        runTest {
            val receivedFrames = LinkedBlockingQueue<String>()
            var serverSocket: WebSocket? = null
            server.enqueue(
                MockResponse().withWebSocketUpgrade(
                    object : WebSocketListener() {
                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response,
                        ) {
                            serverSocket = webSocket
                        }

                        override fun onMessage(
                            webSocket: WebSocket,
                            text: String,
                        ) {
                            receivedFrames += text
                            if (text.startsWith("CONNECT")) {
                                webSocket.send(StompCodec.buildFrame("CONNECTED", linkedMapOf("version" to "1.2"), ""))
                            }
                        }
                    },
                ),
            )

            val client = OkHttpChatRealtimeClient(okHttpClient, NoOpLogger)
            val connected = async { client.events.filterIsInstance<ChatRealtimeEvent.Connected>().first() }
            val subscribed = async { client.events.filterIsInstance<ChatRealtimeEvent.Subscribed>().first() }
            val message = async { client.events.filterIsInstance<ChatRealtimeEvent.TextMessage>().first() }

            client.connect(server.url("/ws").toString().replace("http://", "ws://"), "token-123")
            connected.await()
            val connectFrame = receivedFrames.poll(2, TimeUnit.SECONDS)
            assertTrue(connectFrame.contains("Authorization:Bearer token-123"))

            client.subscribe("/topic/chatrooms/chat-1")
            subscribed.await()
            val subscribeFrame = receivedFrames.poll(2, TimeUnit.SECONDS)
            assertTrue(subscribeFrame.contains("SUBSCRIBE"))
            assertTrue(subscribeFrame.contains("destination:/topic/chatrooms/chat-1"))

            assertTrue(client.send("/app/chat/sendMessage", """{"chatRoomId":"chat-1"}"""))
            val sendFrame = receivedFrames.poll(2, TimeUnit.SECONDS)
            assertTrue(sendFrame.contains("SEND"))
            assertTrue(sendFrame.contains("destination:/app/chat/sendMessage"))
            assertTrue(sendFrame.contains("""{"chatRoomId":"chat-1"}"""))

            serverSocket?.send(
                StompCodec.buildFrame(
                    command = "MESSAGE",
                    headers = linkedMapOf("destination" to "/topic/chatrooms/chat-1"),
                    body = """{"chatRoomId":"chat-1"}""",
                ),
            )

            assertEquals(
                ChatRealtimeEvent.TextMessage(
                    destination = "/topic/chatrooms/chat-1",
                    text = """{"chatRoomId":"chat-1"}""",
                ),
                message.await(),
            )
            client.disconnect()
            serverSocket?.close(SOCKET_CLOSE_NORMAL, "test done")
        }
}

private object NoOpLogger : Logger {
    override fun v(
        tag: String,
        message: String,
    ) = Unit

    override fun d(
        tag: String,
        message: String,
    ) = Unit

    override fun i(
        tag: String,
        message: String,
    ) = Unit

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = Unit

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = Unit
}
