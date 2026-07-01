package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.data.local.entity.ChatEntity
import com.citruschat.citrusmobile.data.local.entity.ChatListItemEntity
import com.citruschat.citrusmobile.data.local.entity.ChatParticipantCrossRef
import com.citruschat.citrusmobile.data.local.entity.ChatReadStateEntity
import com.citruschat.citrusmobile.data.local.entity.MessageEntity
import com.citruschat.citrusmobile.data.local.entity.UserEntity
import com.citruschat.citrusmobile.data.local.entity.type.ChatType
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.model.MessageDeliveryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMapperTest {
    @Test
    fun `maps chat entity to domain`() {
        val entity = ChatEntity(id = 12, name = "General", type = ChatType.GROUP, lastMessageId = 99)

        val domain = entity.toDomain(participantUserIds = listOf("user-1", "user-2"))

        assertEquals(
            Chat(
                id = 12,
                name = "General",
                type = ChatType.GROUP,
                lastMessageId = 99,
                participantUserIds = listOf("user-1", "user-2"),
            ),
            domain,
        )
    }

    @Test
    fun `maps direct chat domain to entity without dedicated avatar`() {
        val domain =
            Chat(
                id = 7,
                name = "Support",
                type = ChatType.DIRECT,
                remoteProfilePictureUrl = "https://example.com/ignored.png",
                localProfilePicturePath = "/local/ignored.png",
            )

        val entity = domain.toEntity()

        assertEquals(
            ChatEntity(id = 7, name = "Support", type = ChatType.DIRECT),
            entity,
        )
    }

    @Test
    fun `maps group chat domain to entity with dedicated avatar`() {
        val domain =
            Chat(
                id = 8,
                name = "Team",
                type = ChatType.GROUP,
                remoteProfilePictureUrl = "https://example.com/team.png",
                localProfilePicturePath = "/local/team.png",
            )

        val entity = domain.toEntity()

        assertEquals(
            ChatEntity(
                id = 8,
                name = "Team",
                type = ChatType.GROUP,
                remoteProfilePictureUrl = "https://example.com/team.png",
                localProfilePicturePath = "/local/team.png",
            ),
            entity,
        )
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
    fun `direct chat summary uses other participant name and local avatar`() {
        val listItem =
            ChatListItemEntity(
                chat = ChatEntity(id = 3, name = "Project", type = ChatType.DIRECT, lastMessageId = 44),
                readState = ChatReadStateEntity(chatId = 3, unreadCount = 2),
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
                            id = "current-user",
                            email = "me@example.com",
                            username = "me",
                            remoteProfilePictureUrl = "https://example.com/me.png",
                            localProfilePicturePath = "/local/me.png",
                        ),
                        UserEntity(
                            id = "user-2",
                            email = "grace@example.com",
                            username = "grace",
                            remoteProfilePictureUrl = "https://example.com/grace.png",
                            localProfilePicturePath = "/local/grace.png",
                        ),
                    ),
            )

        val summary = listItem.toSummary(currentUserId = "current-user")

        assertEquals(
            ChatListItemSummary(
                id = 3,
                name = "grace",
                type = ChatType.DIRECT,
                lastMessagePreview = "See you soon",
                localProfilePicturePath = "/local/grace.png",
                remoteProfilePictureUrl = "https://example.com/grace.png",
                participantUserIds = listOf("current-user", "user-2"),
                participantUsernames = listOf("me", "grace"),
                participantAvatarUrls = listOf("/local/me.png", "/local/grace.png"),
                lastMessageStatus = MessageDeliveryStatus.VIEWED,
                unreadCount = 2,
            ),
            summary,
        )
    }

    @Test
    fun `group chat summary uses dedicated chat avatar`() {
        val listItem =
            ChatListItemEntity(
                chat =
                    ChatEntity(
                        id = 4,
                        name = "Team",
                        type = ChatType.GROUP,
                        remoteProfilePictureUrl = "https://example.com/team.png",
                        localProfilePicturePath = "/local/team.png",
                    ),
                readState = null,
                lastMessage = null,
            )

        val summary = listItem.toSummary(currentUserId = "current-user")

        assertEquals(
            ChatListItemSummary(
                id = 4,
                name = "Team",
                type = ChatType.GROUP,
                lastMessagePreview = null,
                localProfilePicturePath = "/local/team.png",
                remoteProfilePictureUrl = "https://example.com/team.png",
            ),
            summary,
        )
    }
}
