package com.citruschat.citrusmobile.domain.model

data class Message(
    val id: Long,
    val user: String,
    val text: String,
    val isOwn: Boolean,
    val timestamp: Long,
)
