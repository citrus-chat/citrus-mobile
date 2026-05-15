package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.data.local.entity.ChatEntity
import com.citruschat.citrusmobile.domain.model.Chat

fun ChatEntity.toDomain() =
    Chat(
        id = id,
        name = name,
        lastMessageId = lastMessageId,
    )

fun Chat.toEntity() =
    ChatEntity(
        id = id,
        name = name,
        lastMessageId = lastMessageId,
    )
