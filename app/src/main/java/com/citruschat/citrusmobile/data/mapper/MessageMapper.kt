package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.data.local.entity.MessageEntity
import com.citruschat.citrusmobile.domain.model.Message

fun MessageEntity.toDomain() =
    Message(
        id = id,
        user = user,
        text = text,
        isOwn = isOwn,
        timestamp = timestamp,
        chatId = chatId,
    )

fun Message.toEntity() =
    MessageEntity(
        id = id,
        user = user,
        text = text,
        isOwn = isOwn,
        timestamp = timestamp,
        chatId = chatId,
    )
