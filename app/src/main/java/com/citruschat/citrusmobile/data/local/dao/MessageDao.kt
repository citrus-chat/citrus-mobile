package com.citruschat.citrusmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.citruschat.citrusmobile.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("UPDATE chats SET lastMessageId = :messageId WHERE id = :chatId")
    suspend fun updateChatLastMessage(
        chatId: Long,
        messageId: Long,
    )

    @Transaction
    suspend fun insertAndMarkAsLastMessage(message: MessageEntity): Long {
        val messageId = insert(message)
        updateChatLastMessage(chatId = message.chatId, messageId = messageId)
        return messageId
    }

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun observeByChatId(chatId: Long): Flow<List<MessageEntity>>
}
