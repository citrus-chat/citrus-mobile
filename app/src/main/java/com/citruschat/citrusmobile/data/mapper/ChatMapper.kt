package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.data.local.entity.ChatEntity
import com.citruschat.citrusmobile.data.local.entity.ChatListItemEntity
import com.citruschat.citrusmobile.data.local.entity.ChatParticipantCrossRef
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary

fun ChatEntity.toDomain(participantUserIds: List<String> = emptyList()) =
    Chat(
        id = id,
        name = name,
        lastMessageId = lastMessageId,
        participantUserIds = participantUserIds,
    )

fun Chat.toEntity() =
    ChatEntity(
        id = id,
        name = name,
        lastMessageId = lastMessageId,
    )

fun Chat.toParticipantRefs(chatId: Long = id): List<ChatParticipantCrossRef> =
    participantUserIds.map { userId ->
        ChatParticipantCrossRef(
            chatId = chatId,
            userId = userId,
        )
    }

fun ChatListItemEntity.toSummary() =
    ChatListItemSummary(
        id = chat.id,
        name = chat.name,
        lastMessagePreview = lastMessage?.text,
        participantUserIds = participants.map { it.id },
        participantUsernames = participants.map { it.username },
    )
