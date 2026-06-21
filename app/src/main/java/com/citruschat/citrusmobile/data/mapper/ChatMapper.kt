package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.data.local.entity.ChatEntity
import com.citruschat.citrusmobile.data.local.entity.ChatListItemEntity
import com.citruschat.citrusmobile.data.local.entity.ChatParticipantCrossRef
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.model.MessageDeliveryStatus

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
        name = participants.singleOrNull()?.username ?: chat.name,
        lastMessagePreview = lastMessage?.text,
        participantUserIds = participants.map { it.id },
        participantUsernames = participants.map { it.username },
        participantAvatarUrls = participants.map { it.profilePictureUrl },
        lastMessageStatus = lastMessage?.deliveryStatus?.toMessageDeliveryStatus(),
    )

private fun String.toMessageDeliveryStatus(): MessageDeliveryStatus =
    runCatching { MessageDeliveryStatus.valueOf(this) }
        .getOrDefault(MessageDeliveryStatus.SENT)
