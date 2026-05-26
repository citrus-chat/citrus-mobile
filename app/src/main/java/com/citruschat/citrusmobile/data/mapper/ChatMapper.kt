package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.data.local.entity.ChatEntity
import com.citruschat.citrusmobile.data.local.entity.ChatListItemEntity
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary

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

fun ChatListItemEntity.toSummary() =
    ChatListItemSummary(
        id = chat.id,
        name = chat.name,
        lastMessagePreview = lastMessage?.text,
    )
