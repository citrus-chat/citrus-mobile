package com.citruschat.citrusmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import com.citruschat.citrusmobile.data.local.entity.ChatEntity

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(chat: ChatEntity)
}
