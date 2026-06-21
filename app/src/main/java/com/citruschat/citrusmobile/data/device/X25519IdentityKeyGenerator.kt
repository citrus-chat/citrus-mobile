package com.citruschat.citrusmobile.data.device

import org.bouncycastle.math.ec.rfc7748.X25519
import java.security.SecureRandom
import java.util.Base64

class X25519IdentityKeyGenerator(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun generate(): IdentityKeyPair {
        val privateKey = ByteArray(X25519.SCALAR_SIZE)
        val publicKey = ByteArray(X25519.POINT_SIZE)

        X25519.generatePrivateKey(secureRandom, privateKey)
        X25519.generatePublicKey(privateKey, 0, publicKey, 0)

        return IdentityKeyPair(
            privateKey = privateKey.toBase64(),
            publicKey = publicKey.toBase64(),
        )
    }

    private fun ByteArray.toBase64(): String =
        Base64
            .getEncoder()
            .encodeToString(this)
}

data class IdentityKeyPair(
    val privateKey: String,
    val publicKey: String,
)
