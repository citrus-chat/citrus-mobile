package com.citruschat.citrusmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.citruschat.citrusmobile.data.local.entity.ConversationKeyEntity

@Dao
interface ConversationKeyDao {
    @Upsert
    suspend fun upsert(key: ConversationKeyEntity)

    @Upsert
    suspend fun upsertAll(keys: List<ConversationKeyEntity>)

    @Query(
        """
        SELECT *
        FROM conversation_keys
        WHERE conversationId = :conversationId
        ORDER BY keyVersion DESC
        LIMIT 1
        """,
    )
    suspend fun getActiveConversationKey(conversationId: String): ConversationKeyEntity?

    @Query(
        """
        SELECT *
        FROM conversation_keys
        WHERE conversationId = :conversationId AND keyVersion = :keyVersion
        LIMIT 1
        """,
    )
    suspend fun getConversationKey(
        conversationId: String,
        keyVersion: Int,
    ): ConversationKeyEntity?
}
