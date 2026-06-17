package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.data.local.entity.MessageEntity
import com.citruschat.citrusmobile.domain.model.Message
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageMapperTest {
    @Test
    fun `maps message entity to domain`() {
        val entity =
            MessageEntity(
                id = 5,
                user = "Grace",
                text = "Hello",
                isOwn = true,
                timestamp = 1_700_000_001,
                chatId = 9,
            )

        val domain = entity.toDomain()

        assertEquals(
            Message(
                id = 5,
                user = "Grace",
                text = "Hello",
                isOwn = true,
                timestamp = 1_700_000_001,
                chatId = 9,
            ),
            domain,
        )
    }

    @Test
    fun `maps message domain to entity`() {
        val domain =
            Message(
                id = 6,
                user = "Linus",
                text = "Ack",
                isOwn = false,
                timestamp = 1_700_000_002,
                chatId = 10,
            )

        val entity = domain.toEntity()

        assertEquals(
            MessageEntity(
                id = 6,
                user = "Linus",
                text = "Ack",
                isOwn = false,
                timestamp = 1_700_000_002,
                chatId = 10,
            ),
            entity,
        )
    }
}
