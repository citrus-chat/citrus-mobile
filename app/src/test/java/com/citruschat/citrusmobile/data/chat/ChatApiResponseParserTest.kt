package com.citruschat.citrusmobile.data.chat

import com.citruschat.citrusmobile.data.local.entity.type.ChatType
import com.citruschat.citrusmobile.domain.model.ChatPermissions
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatApiResponseParserTest {
    @Test
    fun `parses sync chatrooms response with participants and conversation keys`() {
        val sync =
            ChatApiResponseParser.parseSyncChatRooms(
                """
                {
                  "data": {
                    "chatRooms": [
                      {
                        "id": "chatroom-1",
                        "type": "DIRECT",
                        "name": "ada",
                        "createdBy": "user-1",
                        "participants": [
                          {
                            "id": "participant-1",
                            "chatRoomId": "chatroom-1",
                            "userId": "user-1",
                            "roleIds": [],
                            "joinedAt": "2026-06-25T10:00:00.000Z",
                            "leftAt": null,
                            "lastReadMessageId": null
                          }
                        ],
                        "createdAt": "2026-06-25T10:00:00.000Z",
                        "updatedAt": "2026-06-25T10:01:00.000Z",
                        "deletedAt": null
                      }
                    ],
                    "conversationKeys": [
                      {
                        "conversationId": "chatroom-1",
                        "senderDeviceId": "device-1",
                        "keyVersion": 1,
                        "iv": "iv-base64",
                        "ciphertext": "ciphertext-base64",
                        "createdAt": "2026-06-25T10:00:00.000Z"
                      }
                    ]
                  }
                }
                """.trimIndent(),
            )

        assertEquals(
            RemoteChatRoomSync(
                chatRooms =
                    listOf(
                        RemoteChatRoom(
                            id = "chatroom-1",
                            type = ChatType.DIRECT,
                            name = "ada",
                            createdBy = "user-1",
                            participants =
                                listOf(
                                    RemoteChatParticipant(
                                        id = "participant-1",
                                        chatRoomId = "chatroom-1",
                                        userId = "user-1",
                                    ),
                                ),
                            createdAt = "2026-06-25T10:00:00.000Z",
                            updatedAt = "2026-06-25T10:01:00.000Z",
                            deletedAt = null,
                        ),
                    ),
                conversationKeys =
                    listOf(
                        RemoteConversationKey(
                            conversationId = "chatroom-1",
                            senderDeviceId = "device-1",
                            keyVersion = 1,
                            iv = "iv-base64",
                            ciphertext = "ciphertext-base64",
                            createdAt = "2026-06-25T10:00:00.000Z",
                        ),
                    ),
            ),
            sync,
        )
    }

    @Test
    fun `parses user device keys response`() {
        val keys =
            ChatApiResponseParser.parseUserDeviceKeys(
                """
                {
                  "data": {
                    "userId": "user-2",
                    "devices": [
                      {
                        "deviceId": "device-2",
                        "publicKey": "public-key-base64"
                      }
                    ]
                  }
                }
                """.trimIndent(),
            )

        assertEquals(
            UserDeviceKeys(
                userId = "user-2",
                devices =
                    listOf(
                        DevicePublicKey(
                            userId = "user-2",
                            deviceId = "device-2",
                            publicKey = "public-key-base64",
                        ),
                    ),
            ),
            keys,
        )
    }

    @Test
    fun `parses send-message permission from web permission response`() {
        val permissions =
            ChatApiResponseParser.parsePermissions(
                """
                {
                  "data": {
                    "permissions": [
                      {
                        "permissionId": "permission-1",
                        "code": "CAN_SEND_MESSAGE",
                        "description": "Can send message"
                      },
                      {
                        "permissionId": "permission-2",
                        "code": "CAN_ADD_PARTICIPANTS",
                        "description": "Can add participants"
                      }
                    ]
                  }
                }
                """.trimIndent(),
            )

        assertEquals(
            ChatPermissions(
                canWrite = true,
                canRead = true,
                canAddParticipants = true,
            ),
            permissions,
        )
    }
}
