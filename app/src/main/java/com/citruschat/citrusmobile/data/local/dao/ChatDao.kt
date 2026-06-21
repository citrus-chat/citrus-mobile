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

    @Transaction
    @Query(
        """
        SELECT DISTINCT
            chats.id,
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
            ) AS lastMessageId
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
        WHERE :searchQuery = ''
            OR chats.name LIKE '%' || :searchQuery || '%'
            OR messages.text LIKE '%' || :searchQuery || '%'
            OR users.username LIKE '%' || :searchQuery || '%'
            OR users.email LIKE '%' || :searchQuery || '%'
        ORDER BY chats.id DESC
        """,
    )
    fun observeItems(searchQuery: String): Flow<List<ChatListItemEntity>>
}
