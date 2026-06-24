package com.citruschat.citrusmobile.data.device

import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class P256IdentityKeyGenerator(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun generate(): IdentityKeyPair {
        val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        generator.initialize(ECGenParameterSpec(CURVE_NAME), secureRandom)

        val keyPair = generator.generateKeyPair()
        val publicKey = keyPair.public as ECPublicKey

        return IdentityKeyPair(
            privateKey = keyPair.private.encoded.toBase64(),
            publicKey = publicKey.toRawUncompressedBytes().toBase64(),
        )
    }

    private fun ECPublicKey.toRawUncompressedBytes(): ByteArray {
        val x = w.affineX.toFixedUnsignedBytes(COORDINATE_SIZE_BYTES)
        val y = w.affineY.toFixedUnsignedBytes(COORDINATE_SIZE_BYTES)

        return ByteArray(RAW_PUBLIC_KEY_SIZE_BYTES).also { output ->
            output[0] = UNCOMPRESSED_POINT_PREFIX
            x.copyInto(output, destinationOffset = 1)
            y.copyInto(output, destinationOffset = 1 + COORDINATE_SIZE_BYTES)
        }
    }

    private fun java.math.BigInteger.toFixedUnsignedBytes(size: Int): ByteArray {
        val unsignedBytes = toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
        require(unsignedBytes.size <= size) { "Coordinate is longer than $size bytes" }

        return ByteArray(size).also { output ->
            unsignedBytes.copyInto(output, destinationOffset = size - unsignedBytes.size)
        }
    }

    private fun ByteArray.toBase64(): String =
        Base64
            .getEncoder()
            .encodeToString(this)

    companion object {
        const val RAW_PUBLIC_KEY_SIZE_BYTES = 65
        private const val COORDINATE_SIZE_BYTES = 32
        private const val KEY_ALGORITHM = "EC"
        private const val CURVE_NAME = "secp256r1"
        private const val UNCOMPRESSED_POINT_PREFIX = 0x04.toByte()
    }
}

data class IdentityKeyPair(
    val privateKey: String,
    val publicKey: String,
)
