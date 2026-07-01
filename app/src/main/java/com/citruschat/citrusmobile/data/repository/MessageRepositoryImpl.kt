package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.BuildConfig
import com.citruschat.citrusmobile.app.AppVisibilityTracker
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.data.local.dao.ChatDao
import com.citruschat.citrusmobile.data.local.dao.MessageDao
import com.citruschat.citrusmobile.data.local.dao.UserDao
import com.citruschat.citrusmobile.data.mapper.toDomain
import com.citruschat.citrusmobile.data.mapper.toEntity
import com.citruschat.citrusmobile.data.message.MessageApiClient
import com.citruschat.citrusmobile.data.notification.ChatNotificationNotifier
import com.citruschat.citrusmobile.domain.model.Message
import com.citruschat.citrusmobile.domain.realtime.ChatRealtimeClient
import com.citruschat.citrusmobile.domain.realtime.ChatRealtimeEvent
import com.citruschat.citrusmobile.domain.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

class MessageRepositoryImpl
    @Inject
    constructor(
        private val dao: MessageDao,
        private val chatDao: ChatDao,
        private val userDao: UserDao,
        private val tokenStore: TokenStore,
        private val messageApiClient: MessageApiClient,
        private val realtimeClient: ChatRealtimeClient,
        private val notificationNotifier: ChatNotificationNotifier,
        private val appVisibilityTracker: AppVisibilityTracker,
        private val logger: Logger,
    ) : MessageRepository {
        private val realtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val activeChatId = AtomicLong(NO_ACTIVE_CHAT_ID)
        private val subscribedChatIds = AtomicReference<List<Long>>(emptyList())

        init {
            realtimeScope.launch {
                realtimeClient.events.collect { event ->
                    when (event) {
                        is ChatRealtimeEvent.TextMessage -> {
                            logger.d(TAG, "Realtime notification received destination=${event.destination}")
                            event.destination?.substringAfterLast('/')?.takeIf { it.isNotBlank() }?.let { remoteChatId ->
                                syncRemoteChat(remoteChatId)
                            }
                        }
                        is ChatRealtimeEvent.Disconnected,
                        is ChatRealtimeEvent.Failure,
                        -> {
                            val chatIds = subscribedChatIds.get()
                            if (chatIds.isNotEmpty()) {
                                logger.w(TAG, "Realtime disconnected; scheduling reconnect for chats=${chatIds.size}")
                                delay(RECONNECT_DELAY_MILLIS)
                                startRealtimeForChats(chatIds)
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }

        override fun observeMessages(chatId: Long): Flow<List<Message>> =
            dao
                .observeByChatId(chatId)
                .map { entities ->
                    logger.v(TAG, "Observed messages update for chatId=$chatId count=${entities.size}")
                    entities.map { it.toDomain() }
                }

        override fun observeTotalUnreadCount(): Flow<Int> = chatDao.observeTotalUnreadCount()

        override suspend fun startRealtime(chatId: Long) {
            activeChatId.set(chatId)
            chatDao.markChatRead(chatId)
            val remoteId = chatDao.remoteIdForChat(chatId)
            if (remoteId.isNullOrBlank()) {
                logger.w(TAG, "Realtime not started because chatId=$chatId has no remoteId")
                return
            }
            val token = tokenStore.observeTokens().firstOrNull()?.accessToken
            realtimeClient.connect(BuildConfig.WS_BASE_URL.trimEnd('/') + "/ws", token)
            realtimeClient.subscribe("/topic/chatrooms/$remoteId")
            syncChat(chatId, remoteId, trackUnread = false)
            chatDao.markChatRead(chatId)
        }

        override suspend fun startRealtimeForChats(chatIds: List<Long>) {
            subscribedChatIds.set(chatIds.distinct())
            val token = tokenStore.observeTokens().firstOrNull()?.accessToken
            realtimeClient.connect(BuildConfig.WS_BASE_URL.trimEnd('/') + "/ws", token)
            subscribedChatIds.get().forEach { chatId ->
                val remoteId = chatDao.remoteIdForChat(chatId)
                if (!remoteId.isNullOrBlank()) {
                    realtimeClient.subscribe("/topic/chatrooms/$remoteId")
                    syncChat(chatId, remoteId, trackUnread = true)
                }
            }
        }

        override suspend fun syncMessages(chatId: Long) {
            val remoteId = chatDao.remoteIdForChat(chatId)
            if (remoteId.isNullOrBlank()) {
                logger.w(TAG, "Message sync skipped because chatId=$chatId has no remoteId")
                return
            }
            syncChat(chatId, remoteId, trackUnread = false)
        }

        override suspend fun sendMessage(message: Message) {
            val remoteChatId = chatDao.remoteIdForChat(message.chatId)
            if (remoteChatId.isNullOrBlank()) {
                logger.w(TAG, "Persisting local-only message because chatId=${message.chatId} has no remoteId")
                dao.insertAndMarkAsLastMessage(message.toEntity())
                return
            }

            logger.i(TAG, "Sending message over websocket chatId=${message.chatId} remoteChatId=$remoteChatId")
            val payload = messageApiClient.createSendPayload(remoteChatId, message.text)
            if (payload == null) {
                logger.w(TAG, "Message not sent because encrypted payload could not be created chatId=${message.chatId}")
                return
            }
            val sentOverWebSocket = realtimeClient.send(WS_SEND_MESSAGE_DESTINATION, payload.json)
            val remoteMessageId =
                if (sentOverWebSocket) {
                    payload.messageId
                } else {
                    logger.w(TAG, "WebSocket send unavailable; falling back to REST for chatId=${message.chatId}")
                    messageApiClient.sendMessage(remoteChatId, message.text)
                }

            if (remoteMessageId == null) {
                dao.insertAndMarkAsLastMessage(message.toEntity())
                return
            }

            dao.insertAndMarkAsLastMessage(message.copy(remoteId = remoteMessageId).toEntity())
            syncMessages(message.chatId)
        }

        override fun stopRealtime() {
            activeChatId.set(NO_ACTIVE_CHAT_ID)
        }

        private suspend fun syncRemoteChat(remoteChatId: String) {
            val chatId = chatDao.chatIdForRemoteId(remoteChatId)
            if (chatId == null) {
                logger.w(TAG, "Realtime notification ignored because remoteChatId=$remoteChatId is not local")
                return
            }
            syncChat(chatId, remoteChatId, trackUnread = true)
        }

        private suspend fun syncChat(
            chatId: Long,
            remoteChatId: String,
            trackUnread: Boolean,
        ) {
            val currentUserId = userDao.observeCurrentUser().firstOrNull()?.id
            val latestTimestamp = dao.latestTimestamp(chatId)
            val messages = messageApiClient.syncMessages(chatId, remoteChatId, currentUserId, latestTimestamp)
            val insertedIncomingMessages = mutableListOf<Pair<Long, Message>>()
            messages.forEach { message ->
                val messageId = dao.insertRemoteAndMarkAsLastMessage(message.toEntity())
                if (messageId > 0 && !message.isOwn) {
                    insertedIncomingMessages += messageId to message
                }
            }
            if (trackUnread && insertedIncomingMessages.isNotEmpty()) {
                val isVisibleActiveChat = appVisibilityTracker.isForeground && activeChatId.get() == chatId
                if (isVisibleActiveChat) {
                    chatDao.markFirstUnreadInOpenChat(
                        chatId = chatId,
                        messageId = insertedIncomingMessages.first().first,
                        count = insertedIncomingMessages.size,
                    )
                } else {
                    chatDao.incrementUnreadCount(chatId, insertedIncomingMessages.size)
                }
                if (!appVisibilityTracker.isForeground) {
                    val chatName = chatDao.chatNameForId(chatId).orEmpty()
                    insertedIncomingMessages.lastOrNull()?.second?.let { message ->
                        notificationNotifier.showIncomingMessage(
                            chatId = chatId,
                            chatName = chatName,
                            message = message,
                        )
                    }
                }
            }
            logger.i(TAG, "Message sync finished chatId=$chatId count=${messages.size}")
        }
    }

private const val NO_ACTIVE_CHAT_ID = -1L
private const val RECONNECT_DELAY_MILLIS = 1_000L
private const val WS_SEND_MESSAGE_DESTINATION = "/app/chat/sendMessage"
private const val TAG = "MessageRepository"
