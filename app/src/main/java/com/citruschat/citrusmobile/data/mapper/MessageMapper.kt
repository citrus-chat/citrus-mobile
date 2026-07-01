package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.data.local.entity.MessageEntity
import com.citruschat.citrusmobile.domain.model.Message
import com.citruschat.citrusmobile.domain.model.MessageDeliveryStatus

fun MessageEntity.toDomain() =
    Message(
        id = id,
        remoteId = remoteId,
        user = user,
        text = text,
        isOwn = isOwn,
        timestamp = timestamp,
        chatId = chatId,
        deliveryStatus = deliveryStatus.toMessageDeliveryStatus(),
    )

fun Message.toEntity() =
    MessageEntity(
        id = id,
        remoteId = remoteId,
        user = user,
        text = text,
        isOwn = isOwn,
        timestamp = timestamp,
        chatId = chatId,
        deliveryStatus = deliveryStatus.name,
    )

private fun String.toMessageDeliveryStatus(): MessageDeliveryStatus =
    runCatching { MessageDeliveryStatus.valueOf(this) }
        .getOrDefault(MessageDeliveryStatus.SENT)
