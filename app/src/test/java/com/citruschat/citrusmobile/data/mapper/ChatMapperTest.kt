package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.data.local.entity.ChatEntity
import com.citruschat.citrusmobile.data.local.entity.ChatListItemEntity
import com.citruschat.citrusmobile.data.local.entity.ChatParticipantCrossRef
import com.citruschat.citrusmobile.data.local.entity.MessageEntity
import com.citruschat.citrusmobile.data.local.entity.UserEntity
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.model.MessageDeliveryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMapperTest {
    @Test
    fun `maps chat entity to domain`() {
        val entity = ChatEntity(id = 12, name = "General", lastMessageId = 99)

        val domain = entity.toDomain(participantUserIds = listOf("user-1", "user-2"))

        assertEquals(
            Chat(
                id = 12,
                name = "General",
                lastMessageId = 99,
                participantUserIds = listOf("user-1", "user-2"),
            ),
            domain,
        )
    }

    @Test
    fun `maps chat domain to entity`() {
        val domain = Chat(id = 7, name = "Support", lastMessageId = null)

        val entity = domain.toEntity()

        assertEquals(ChatEntity(id = 7, name = "Support", lastMessageId = null), entity)
    }

    @Test
    fun `maps chat domain participants to cross refs`() {
        val domain = Chat(id = 7, name = "Support", participantUserIds = listOf("user-1", "user-2"))

        val refs = domain.toParticipantRefs()

        assertEquals(
            listOf(
                ChatParticipantCrossRef(chatId = 7, userId = "user-1"),
                ChatParticipantCrossRef(chatId = 7, userId = "user-2"),
            ),
            refs,
        )
    }

    @Test
    fun `maps chat list item with last message preview and participants`() {
        val listItem =
            ChatListItemEntity(
                chat = ChatEntity(id = 3, name = "Project", lastMessageId = 44),
                lastMessage =
                    MessageEntity(
                        id = 44,
                        user = "Ada",
                        text = "See you soon",
                        isOwn = false,
                        timestamp = 1_700_000_000,
                        chatId = 3,
                        deliveryStatus = "VIEWED",
                    ),
                participants =
                    listOf(
                        UserEntity(
                            id = "user-1",
                            email = "ada@example.com",
                            username = "ada",
                            profilePictureUrl = "https://example.com/ada.png",
                        ),
                        UserEntity(
                            id = "user-2",
                            email = "grace@example.com",
                            username = "grace",
                        ),
                    ),
            )

        val summary = listItem.toSummary()

        assertEquals(
            ChatListItemSummary(
                id = 3,
                name = "Project",
                lastMessagePreview = "See you soon",
                participantUserIds = listOf("user-1", "user-2"),
                participantUsernames = listOf("ada", "grace"),
                participantAvatarUrls = listOf("https://example.com/ada.png", null),
                lastMessageStatus = MessageDeliveryStatus.VIEWED,
            ),
            summary,
        )
    }

    @Test
    fun `maps chat list item without last message preview`() {
        val listItem =
            ChatListItemEntity(
                chat = ChatEntity(id = 4, name = "Empty", lastMessageId = null),
                lastMessage = null,
            )

        val summary = listItem.toSummary()

        assertEquals(
            ChatListItemSummary(
                id = 4,
                name = "Empty",
                lastMessagePreview = null,
            ),
            summary,
        )
    }
}
