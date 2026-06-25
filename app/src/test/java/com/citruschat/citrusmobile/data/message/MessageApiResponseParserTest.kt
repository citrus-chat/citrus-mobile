package com.citruschat.citrusmobile.data.message

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageApiResponseParserTest {
    @Test
    fun `parses encrypted sync messages response`() {
        val messages =
            MessageApiResponseParser.parseSyncMessages(
                """
                {
                  "data": {
                    "messages": [
                      {
                        "id": "message-1",
                        "chatRoomId": "chatroom-1",
                        "senderUserId": "user-1",
                        "senderDeviceId": "device-1",
                        "replyToMessageId": null,
                        "keyVersion": 1,
                        "iv": "iv-base64",
                        "ciphertext": "ciphertext-base64",
                        "createdAt": "2026-06-25T10:00:00.123456Z",
                        "editedAt": null,
                        "deletedAt": null
                      }
                    ]
                  }
                }
                """.trimIndent(),
            )

        assertEquals(
            listOf(
                RemoteEncryptedMessage(
                    id = "message-1",
                    chatRoomId = "chatroom-1",
                    senderUserId = "user-1",
                    senderDeviceId = "device-1",
                    replyToMessageId = null,
                    keyVersion = 1,
                    iv = "iv-base64",
                    ciphertext = "ciphertext-base64",
                    createdAt = "2026-06-25T10:00:00.123456Z",
                    editedAt = null,
                    deletedAt = null,
                ),
            ),
            messages,
        )
    }

    @Test
    fun `parses optional reply edited and deleted timestamps`() {
        val messages =
            MessageApiResponseParser.parseSyncMessages(
                """
                {
                  "data": {
                    "messages": [
                      {
                        "id": "message-2",
                        "chatRoomId": "chatroom-1",
                        "senderUserId": "user-2",
                        "senderDeviceId": "device-2",
                        "replyToMessageId": "message-1",
                        "keyVersion": 2,
                        "iv": "iv-2",
                        "ciphertext": "ciphertext-2",
                        "createdAt": "2026-06-25T10:02:00.000Z",
                        "editedAt": "2026-06-25T10:03:00.000Z",
                        "deletedAt": "2026-06-25T10:04:00.000Z"
                      }
                    ]
                  }
                }
                """.trimIndent(),
            )

        assertEquals("message-1", messages.single().replyToMessageId)
        assertEquals("2026-06-25T10:03:00.000Z", messages.single().editedAt)
        assertEquals("2026-06-25T10:04:00.000Z", messages.single().deletedAt)
    }
}
