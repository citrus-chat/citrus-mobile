package com.citruschat.citrusmobile.data.user

import com.citruschat.citrusmobile.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class UserApiResponseParserTest {
    @Test
    fun `parses avatar url from strict user response`() {
        val users =
            UserApiResponseParser.parseUsers(
                """
                {
                  "data": [
                    {
                      "id": "user-1",
                      "email": "ada@example.com",
                      "username": "ada",
                      "avatar_url": "https://example.com/ada.png"
                    }
                  ]
                }
                """.trimIndent(),
            )

        assertEquals(
            listOf(
                User(
                    id = "user-1",
                    email = "ada@example.com",
                    username = "ada",
                    remoteProfilePictureUrl = "https://example.com/ada.png",
                ),
            ),
            users,
        )
    }

    @Test
    fun `parses current user avatar url from strict current user response`() {
        val user =
            UserApiResponseParser.parseCurrentUser(
                """
                {
                  "data": {
                    "userId": "user-1",
                    "email": "ada@example.com",
                    "username": "ada",
                    "avatar_url": "http://localhost:8200/api/v1/users/avatars/avatar.png"
                  }
                }
                """.trimIndent(),
            )

        assertEquals(
            User(
                id = "user-1",
                email = "ada@example.com",
                username = "ada",
                remoteProfilePictureUrl = "http://localhost:8200/api/v1/users/avatars/avatar.png",
                isCurrentUser = true,
            ),
            user,
        )
    }

    @Test
    fun `parses avatar url from strict avatar response`() {
        val avatarUrl =
            UserApiResponseParser.parseAvatarUrl(
                """
                {
                  "data": {
                    "avatar_url": "http://localhost:8200/api/v1/users/avatars/avatar.png"
                  }
                }
                """.trimIndent(),
            )

        assertEquals("http://localhost:8200/api/v1/users/avatars/avatar.png", avatarUrl)
    }
}
