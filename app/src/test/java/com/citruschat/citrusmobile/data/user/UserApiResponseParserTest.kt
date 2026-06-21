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
                    profilePictureUrl = "https://example.com/ada.png",
                ),
            ),
            users,
        )
    }
}
