package com.citruschat.citrusmobile.data.crypto

import com.citruschat.citrusmobile.data.device.P256IdentityKeyGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChatCryptoServiceTest {
    private val service = ChatCryptoService()

    @Test
    fun encryptDecrypt_roundTripsMessageWithConversationKey() {
        val conversationKey = service.generateConversationKey()
        val encrypted = service.encrypt("hello from mobile", conversationKey)

        val decrypted = service.decrypt(encrypted, conversationKey)

        assertEquals("hello from mobile", decrypted)
        assertNotEquals("hello from mobile", encrypted.ciphertext)
    }

    @Test
    fun encryptDecryptConversationKey_roundTripsWithP256DeviceKeys() {
        val generator = P256IdentityKeyGenerator()
        val sender = generator.generate()
        val recipient = generator.generate()
        val conversationKey = service.generateConversationKey()

        val encrypted =
            service.encryptConversationKeyForUser(
                conversationKey = conversationKey,
                userPublicKey = recipient.publicKey,
                myPrivateKey = sender.privateKey,
            )

        val decrypted =
            service.decryptConversationKeyForUser(
                payload = encrypted,
                senderPublicKey = sender.publicKey,
                myPrivateKey = recipient.privateKey,
            )

        assertEquals(conversationKey, decrypted)
    }
}
