package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.data.local.entity.ChatEntity
import com.citruschat.citrusmobile.data.local.entity.ChatListItemEntity
import com.citruschat.citrusmobile.data.local.entity.ChatParticipantCrossRef
import com.citruschat.citrusmobile.data.local.entity.type.ChatType
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.model.MessageDeliveryStatus

fun ChatEntity.toDomain(participantUserIds: List<String> = emptyList()) =
    Chat(
        id = id,
        remoteId = remoteId,
        name = name,
        lastMessageId = lastMessageId,
        participantUserIds = participantUserIds,
        localProfilePicturePath = localProfilePicturePath,
        remoteProfilePictureUrl = remoteProfilePictureUrl,
        type = type,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Chat.toEntity() =
    ChatEntity(
        id = id,
        remoteId = remoteId,
        name = name,
        lastMessageId = lastMessageId,
        remoteProfilePictureUrl =
            remoteProfilePictureUrl.takeIf { type == ChatType.GROUP },
        localProfilePicturePath =
            localProfilePicturePath.takeIf { type == ChatType.GROUP },
        type = type,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Chat.toParticipantRefs(chatId: Long = id): List<ChatParticipantCrossRef> =
    participantUserIds.map { userId ->
        ChatParticipantCrossRef(
            chatId = chatId,
            userId = userId,
        )
    }

fun ChatListItemEntity.toSummary(currentUserId: String?) =
    ChatListItemSummary(
        id = chat.id,
        remoteId = chat.remoteId,
        name = displayName(currentUserId),
        type = chat.type,
        lastMessagePreview = lastMessage?.text,
        localProfilePicturePath = localProfilePicturePath(currentUserId),
        remoteProfilePictureUrl = remoteProfilePictureUrl(currentUserId),
        participantUserIds = participants.map { it.id },
        participantUsernames = participants.map { it.username },
        participantAvatarUrls = participants.map { it.localProfilePicturePath },
        lastMessageStatus = lastMessage?.deliveryStatus?.toMessageDeliveryStatus(),
    )

private fun ChatListItemEntity.displayName(currentUserId: String?): String =
    when (chat.type) {
        ChatType.DIRECT -> otherParticipant(currentUserId)?.username ?: participants.singleOrNull()?.username ?: chat.name
        ChatType.GROUP -> chat.name
    }

private fun ChatListItemEntity.localProfilePicturePath(currentUserId: String?): String? =
    when (chat.type) {
        ChatType.DIRECT -> otherParticipant(currentUserId)?.localProfilePicturePath
        ChatType.GROUP -> chat.localProfilePicturePath
    }

private fun ChatListItemEntity.remoteProfilePictureUrl(currentUserId: String?): String? =
    when (chat.type) {
        ChatType.DIRECT -> otherParticipant(currentUserId)?.remoteProfilePictureUrl
        ChatType.GROUP -> chat.remoteProfilePictureUrl
    }

private fun ChatListItemEntity.otherParticipant(currentUserId: String?): com.citruschat.citrusmobile.data.local.entity.UserEntity? =
    participants.firstOrNull { participant -> currentUserId != null && participant.id != currentUserId }
        ?: participants.firstOrNull()

private fun String.toMessageDeliveryStatus(): MessageDeliveryStatus =
    runCatching { MessageDeliveryStatus.valueOf(this) }
        .getOrDefault(MessageDeliveryStatus.SENT)
