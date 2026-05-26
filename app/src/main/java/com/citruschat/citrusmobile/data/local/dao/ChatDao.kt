package com.citruschat.citrusmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.citruschat.citrusmobile.data.local.entity.ChatEntity
import com.citruschat.citrusmobile.data.local.entity.ChatListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :chatId")
    fun deleteById(chatId: Long)

    @Transaction
    @Query("SELECT * FROM chats ORDER BY id DESC") // later change to last message timestamp
    fun observeAllItems(): Flow<List<ChatListItemEntity>>
}
