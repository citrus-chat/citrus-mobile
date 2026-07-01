package com.citruschat.citrusmobile.data.crypto

import com.citruschat.citrusmobile.data.device.P256IdentityKeyGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ConversationCryptoTest {
    private val crypto = ConversationCrypto()
    private val keyGenerator = P256IdentityKeyGenerator()

    @Test
    fun `encryptMessage decrypts with same conversation key`() {
        val conversationKey = crypto.generateConversationKey()
        val plaintext = "hello from mobile"

        val encrypted = crypto.encryptMessage(plaintext, conversationKey)
        val decrypted = crypto.decryptMessage(encrypted, conversationKey)

        assertEquals(plaintext, decrypted)
        assertNotEquals(plaintext, encrypted.ciphertext)
    }

    @Test
    fun `conversation key encrypted for another device decrypts with recipient private key`() {
        val sender = keyGenerator.generate()
        val recipient = keyGenerator.generate()
        val conversationKey = crypto.generateConversationKey()

        val encrypted =
            crypto.encryptConversationKeyForDevice(
                conversationKey = conversationKey,
                targetPublicKey = recipient.publicKey,
                senderPrivateKey = sender.privateKey,
            )

        val decrypted =
            crypto.decryptConversationKeyFromDevice(
                payload = encrypted,
                senderPublicKey = sender.publicKey,
                targetPrivateKey = recipient.privateKey,
            )

        assertEquals(conversationKey, decrypted)
        assertNotEquals(conversationKey, encrypted.ciphertext)
    }
}
