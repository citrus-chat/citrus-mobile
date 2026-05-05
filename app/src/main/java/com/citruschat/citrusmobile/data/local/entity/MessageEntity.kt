package com.citruschat.citrusmobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val user: String,
    val text: String,
    val isOwn: Boolean,
    val timestamp: Long,
)
