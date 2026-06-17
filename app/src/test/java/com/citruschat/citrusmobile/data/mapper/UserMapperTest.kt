package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.data.local.entity.UserEntity
import com.citruschat.citrusmobile.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class UserMapperTest {
    @Test
    fun `maps user entity to domain`() {
        val entity =
            UserEntity(
                id = "user-1",
                email = "user@example.com",
                username = "user",
                profilePictureUrl = "https://example.com/avatar.png",
                statusMessage = "Available",
                createdAt = "2026-06-16T10:00:00Z",
                isCurrentUser = true,
            )

        val domain = entity.toDomain()

        assertEquals(
            User(
                id = "user-1",
                email = "user@example.com",
                username = "user",
                profilePictureUrl = "https://example.com/avatar.png",
                statusMessage = "Available",
                createdAt = "2026-06-16T10:00:00Z",
                isCurrentUser = true,
            ),
            domain,
        )
    }

    @Test
    fun `maps user domain to entity`() {
        val domain =
            User(
                id = "user-2",
                email = "second@example.com",
                username = "second",
                profilePictureUrl = null,
                statusMessage = null,
                createdAt = "2026-06-17T10:00:00Z",
                isCurrentUser = false,
            )

        val entity = domain.toEntity()

        assertEquals(
            UserEntity(
                id = "user-2",
                email = "second@example.com",
                username = "second",
                profilePictureUrl = null,
                statusMessage = null,
                createdAt = "2026-06-17T10:00:00Z",
                isCurrentUser = false,
            ),
            entity,
        )
    }
}
