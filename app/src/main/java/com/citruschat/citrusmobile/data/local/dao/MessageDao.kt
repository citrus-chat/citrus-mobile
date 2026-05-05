package com.citruschat.citrusmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.citruschat.citrusmobile.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun observeMessages(): Flow<List<MessageEntity>>

    @Insert
    suspend fun insert(message: MessageEntity)
}
