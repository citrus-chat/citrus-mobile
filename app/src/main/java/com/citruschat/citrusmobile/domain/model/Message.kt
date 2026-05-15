package com.citruschat.citrusmobile.domain.model

data class Message(
    val id: Long = 0,
    val user: String,
    val text: String,
    val timestamp: Long,
    val isOwn: Boolean = false,
    val chatId: Long = 0,
)
