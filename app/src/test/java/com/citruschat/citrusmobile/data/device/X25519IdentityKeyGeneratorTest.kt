package com.citruschat.citrusmobile.data.device

import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.SecureRandom
import java.util.Base64

class X25519IdentityKeyGeneratorTest {
    @Test
    fun `generates raw X25519 public key from private scalar`() {
        val generator =
            X25519IdentityKeyGenerator(
                secureRandom =
                    FixedSecureRandom(
                        "a546e36bf0527c9d3b16154b82465edd62144c0ac1fc5a18506a2244ba449ac4".hexToBytes(),
                    ),
            )

        val keyPair = generator.generate()

        assertEquals(
            "HJ/Yj0VgbZMqgMcYJK4VHRXXPnfeOOjgAIUuYU+ucBk=",
            keyPair.publicKey,
        )
        assertEquals(32, Base64.getDecoder().decode(keyPair.privateKey).size)
        assertEquals(32, Base64.getDecoder().decode(keyPair.publicKey).size)
    }
}

private class FixedSecureRandom(
    private val bytes: ByteArray,
) : SecureRandom() {
    override fun nextBytes(target: ByteArray) {
        bytes.copyInto(target)
    }
}

private fun String.hexToBytes(): ByteArray =
    chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
