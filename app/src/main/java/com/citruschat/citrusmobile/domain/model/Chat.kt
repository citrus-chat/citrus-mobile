package com.citruschat.citrusmobile.domain.model

data class Chat(
    val id: Long = 0,
    val name: String = "",
    val lastMessageId: Long? = null,
)
