package com.citruschat.citrusmobile.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

class P256IdentityKeyGeneratorTest {
    @Test
    fun `generates WebCrypto compatible P-256 identity key pair`() {
        val keyPair = P256IdentityKeyGenerator().generate()

        val publicKey = Base64.getDecoder().decode(keyPair.publicKey)
        val privateKey = Base64.getDecoder().decode(keyPair.privateKey)

        assertEquals(P256IdentityKeyGenerator.RAW_PUBLIC_KEY_SIZE_BYTES, publicKey.size)
        assertEquals(0x04.toByte(), publicKey.first())
        assertTrue(privateKey.isNotEmpty())
        assertEquals(
            "EC",
            KeyFactory
                .getInstance("EC")
                .generatePrivate(PKCS8EncodedKeySpec(privateKey))
                .algorithm,
        )
    }
}
