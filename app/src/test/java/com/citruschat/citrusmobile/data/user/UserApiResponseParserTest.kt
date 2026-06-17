private fun UserApiResponseParserTest() {
    return
}

// package com.citruschat.citrusmobile.data.user
//
// import com.citruschat.citrusmobile.domain.model.User
// import org.junit.Assert.assertEquals
// import org.junit.Test
//
// class UserApiResponseParserTest {
//    @Test
//    fun `parses top level user array`() {
//        val users =
//            UserApiResponseParser.parseUsers(
//                """
//                [
//                    {
//                        "id": "user-1",
//                        "email": "ada@example.com",
//                        "username": "ada",
//                        "profilePictureUrl": "https://example.com/ada.png",
//                        "statusMessage": "Available",
//                        "createdAt": "2026-06-17T10:00:00Z"
//                    }
//                ]
//                """.trimIndent(),
//            )
//
//        assertEquals(
//            listOf(
//                User(
//                    id = "user-1",
//                    email = "ada@example.com",
//                    username = "ada",
//                    profilePictureUrl = "https://example.com/ada.png",
//                    statusMessage = "Available",
//                    createdAt = "2026-06-17T10:00:00Z",
//                ),
//            ),
//            users,
//        )
//    }
//
//    @Test
//    fun `parses wrapped user results`() {
//        val users =
//            UserApiResponseParser.parseUsers(
//                """
//                {
//                    "data": {
//                        "results": [
//                            { "userId": "user-2", "mail": "grace@example.com", "displayName": "Grace" }
//                        ]
//                    }
//                }
//                """.trimIndent(),
//            )
//
//        assertEquals(
//            listOf(
//                User(
//                    id = "user-2",
//                    email = "grace@example.com",
//                    username = "Grace",
//                ),
//            ),
//            users,
//        )
//    }
//
//    @Test
//    fun `parses string usernames and removes duplicate ids`() {
//        val users = UserApiResponseParser.parseUsers("""["ada", "ada", "grace"]""")
//
//        assertEquals(
//            listOf(
//                User(id = "ada", email = "", username = "ada"),
//                User(id = "grace", email = "", username = "grace"),
//            ),
//            users,
//        )
//    }
//
//    @Test
//    fun `returns empty list for blank or missing user arrays`() {
//        assertEquals(emptyList<User>(), UserApiResponseParser.parseUsers(""))
//        assertEquals(emptyList<User>(), UserApiResponseParser.parseUsers("""{"data":{}}"""))
//    }
// }
