package com.citruschat.citrusmobile.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "conversation_keys",
    primaryKeys = ["conversationId", "keyVersion"],
)
data class ConversationKeyEntity(
    val conversationId: String,
    val keyVersion: Int,
    val key: String,
    val createdAt: String,
)
