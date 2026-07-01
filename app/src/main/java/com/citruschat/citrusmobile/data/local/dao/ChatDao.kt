package com.citruschat.citrusmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.citruschat.citrusmobile.data.local.entity.ChatEntity
import com.citruschat.citrusmobile.data.local.entity.ChatListItemEntity
import com.citruschat.citrusmobile.data.local.entity.ChatParticipantCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(chat: ChatEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ChatParticipantCrossRef>)

    @Query("DELETE FROM chats WHERE id = :chatId")
    fun deleteById(chatId: Long)

    @Query(
        """
        SELECT chat_participants.chatId
        FROM chat_participants
        WHERE chat_participants.userId IN (:participantUserIds)
        GROUP BY chat_participants.chatId
        HAVING COUNT(DISTINCT chat_participants.userId) = :participantCount
            AND (
                SELECT COUNT(*)
                FROM chat_participants AS all_participants
                WHERE all_participants.chatId = chat_participants.chatId
            ) = :participantCount
        LIMIT 1
        """,
    )
    suspend fun findDirectChatId(
        participantUserIds: List<String>,
        participantCount: Int,
    ): Long?

    @Query(
        """
        UPDATE chats
        SET localProfilePicturePath = :path
        WHERE id = :chatId
        """,
    )
    suspend fun updateLocalProfilePicturePath(
        chatId: Long,
        path: String,
    )

    @Transaction
    @Query(
        """
        SELECT DISTINCT
            chats.id,
            chats.remoteId,
            chats.name,
            COALESCE(
                chats.lastMessageId,
                (
                    SELECT latest_messages.id
                    FROM messages AS latest_messages
                    WHERE latest_messages.chatId = chats.id
                    ORDER BY latest_messages.timestamp DESC, latest_messages.id DESC
                    LIMIT 1
                )
            ) AS lastMessageId,
            chats.localProfilePicturePath,
            chats.remoteProfilePictureUrl,
            chats.type,
            chat_read_state.chatId AS readState_chatId,
            chat_read_state.unreadCount AS readState_unreadCount,
            chat_read_state.firstUnreadMessageId AS readState_firstUnreadMessageId
        FROM chats
        LEFT JOIN messages ON messages.id = COALESCE(
            chats.lastMessageId,
            (
                SELECT latest_messages.id
                FROM messages AS latest_messages
                WHERE latest_messages.chatId = chats.id
                ORDER BY latest_messages.timestamp DESC, latest_messages.id DESC
                LIMIT 1
            )
        )
        LEFT JOIN chat_participants ON chat_participants.chatId = chats.id
        LEFT JOIN users ON users.id = chat_participants.userId
        LEFT JOIN chat_read_state ON chat_read_state.chatId = chats.id
        WHERE :searchQuery = ''
            OR chats.name LIKE '%' || :searchQuery || '%'
            OR messages.text LIKE '%' || :searchQuery || '%'
            OR users.username LIKE '%' || :searchQuery || '%'
            OR users.email LIKE '%' || :searchQuery || '%'
        ORDER BY chats.id DESC
        """,
    )
    fun observeItems(searchQuery: String): Flow<List<ChatListItemEntity>>

    @Transaction
    @Query(
        """
        SELECT
            chats.id,
            chats.remoteId,
            chats.name,
            chats.lastMessageId,
            chats.localProfilePicturePath,
            chats.remoteProfilePictureUrl,
            chats.type,
            chat_read_state.chatId AS readState_chatId,
            chat_read_state.unreadCount AS readState_unreadCount,
            chat_read_state.firstUnreadMessageId AS readState_firstUnreadMessageId
        FROM chats
        LEFT JOIN chat_read_state ON chat_read_state.chatId = chats.id
        WHERE chats.id = :chatId
        LIMIT 1
        """,
    )
    fun observeItem(chatId: Long): Flow<ChatListItemEntity?>

    @Query("SELECT remoteId FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun remoteIdForChat(chatId: Long): String?

    @Query("SELECT name FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun chatNameForId(chatId: Long): String?

    @Query("UPDATE chats SET remoteId = :remoteId WHERE id = :chatId")
    suspend fun updateRemoteId(
        chatId: Long,
        remoteId: String,
    )

    @Query("SELECT id FROM chats WHERE remoteId = :remoteId LIMIT 1")
    suspend fun chatIdForRemoteId(remoteId: String): Long?

    @Query(
        """
        INSERT INTO chat_read_state(chatId, unreadCount, firstUnreadMessageId)
        VALUES(:chatId, 0, NULL)
        ON CONFLICT(chatId) DO UPDATE SET
            unreadCount = 0,
            firstUnreadMessageId = NULL
        """,
    )
    suspend fun markChatRead(chatId: Long)

    @Query(
        """
        INSERT INTO chat_read_state(chatId, unreadCount, firstUnreadMessageId)
        VALUES(:chatId, :count, NULL)
        ON CONFLICT(chatId) DO UPDATE SET
            unreadCount = unreadCount + :count
        """,
    )
    suspend fun incrementUnreadCount(
        chatId: Long,
        count: Int,
    )

    @Query(
        """
        INSERT INTO chat_read_state(chatId, unreadCount, firstUnreadMessageId)
        VALUES(:chatId, :count, :messageId)
        ON CONFLICT(chatId) DO UPDATE SET
            unreadCount = unreadCount + :count,
            firstUnreadMessageId = COALESCE(firstUnreadMessageId, :messageId)
        """,
    )
    suspend fun markFirstUnreadInOpenChat(
        chatId: Long,
        messageId: Long,
        count: Int,
    )

    @Query("SELECT COALESCE(SUM(unreadCount), 0) FROM chat_read_state")
    fun observeTotalUnreadCount(): Flow<Int>
}
