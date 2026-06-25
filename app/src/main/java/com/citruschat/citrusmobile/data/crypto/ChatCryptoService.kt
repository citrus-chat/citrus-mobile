package com.citruschat.citrusmobile.data.crypto

import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatCryptoService
    @Inject
    constructor() {
        private val secureRandom = SecureRandom()

        fun generateConversationKey(): String {
            val key = ByteArray(AES_KEY_SIZE_BYTES)
            secureRandom.nextBytes(key)
            return key.toBase64()
        }

        fun encrypt(
            plaintext: String,
            key: String,
        ): EncryptedPayload {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val iv = ByteArray(GCM_IV_SIZE_BYTES)
            secureRandom.nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, key.toAesKey(), GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))

            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            return EncryptedPayload(
                iv = iv.toBase64(),
                ciphertext = ciphertext.toBase64(),
            )
        }

        fun decrypt(
            payload: EncryptedPayload,
            key: String,
        ): String {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key.toAesKey(),
                GCMParameterSpec(GCM_TAG_SIZE_BITS, payload.iv.fromBase64()),
            )

            return cipher
                .doFinal(payload.ciphertext.fromBase64())
                .toString(Charsets.UTF_8)
        }

        fun encryptConversationKeyForUser(
            conversationKey: String,
            userPublicKey: String,
            myPrivateKey: String,
        ): EncryptedPayload {
            val wrappingKey = deriveAesKeyFromSharedSecret(myPrivateKey, userPublicKey)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val iv = ByteArray(GCM_IV_SIZE_BYTES)
            secureRandom.nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))

            return EncryptedPayload(
                iv = iv.toBase64(),
                ciphertext = cipher.doFinal(conversationKey.toByteArray(Charsets.UTF_8)).toBase64(),
            )
        }

        fun decryptConversationKeyForUser(
            payload: EncryptedPayload,
            senderPublicKey: String,
            myPrivateKey: String,
        ): String {
            val wrappingKey = deriveAesKeyFromSharedSecret(myPrivateKey, senderPublicKey)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                wrappingKey,
                GCMParameterSpec(GCM_TAG_SIZE_BITS, payload.iv.fromBase64()),
            )

            return cipher
                .doFinal(payload.ciphertext.fromBase64())
                .toString(Charsets.UTF_8)
        }

        private fun deriveAesKeyFromSharedSecret(
            privateKey: String,
            publicKey: String,
        ): SecretKey {
            val keyFactory = KeyFactory.getInstance(EC_ALGORITHM)
            val private = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKey.fromBase64()))
            val public = keyFactory.generatePublic(publicKey.toPublicKeySpec())

            val agreement = KeyAgreement.getInstance(ECDH_ALGORITHM)
            agreement.init(private)
            agreement.doPhase(public, true)

            return SecretKeySpec(
                hkdfSha256(
                    inputKeyMaterial = agreement.generateSecret(),
                    info = HKDF_INFO.toByteArray(Charsets.UTF_8),
                    outputLength = AES_KEY_SIZE_BYTES,
                ),
                AES_ALGORITHM,
            )
        }

        private fun String.toPublicKeySpec(): ECPublicKeySpec {
            val raw = fromBase64()
            require(raw.size == RAW_PUBLIC_KEY_SIZE_BYTES && raw[0] == UNCOMPRESSED_POINT_PREFIX) {
                "Unsupported P-256 public key format"
            }

            val params =
                AlgorithmParameters
                    .getInstance(EC_ALGORITHM)
                    .apply { init(ECGenParameterSpec(CURVE_NAME)) }
            val ecSpec = params.getParameterSpec(ECParameterSpec::class.java)
            val point =
                ECPoint(
                    BigInteger(1, raw.copyOfRange(1, 1 + COORDINATE_SIZE_BYTES)),
                    BigInteger(1, raw.copyOfRange(1 + COORDINATE_SIZE_BYTES, RAW_PUBLIC_KEY_SIZE_BYTES)),
                )
            return ECPublicKeySpec(point, ecSpec)
        }

        private fun hkdfSha256(
            inputKeyMaterial: ByteArray,
            info: ByteArray,
            outputLength: Int,
        ): ByteArray {
            val pseudoRandomKey = hmacSha256(ByteArray(HKDF_HASH_SIZE_BYTES), inputKeyMaterial)
            val output = ByteArray(outputLength)
            var previous = ByteArray(0)
            var offset = 0
            var counter = 1

            while (offset < outputLength) {
                previous =
                    hmacSha256(
                        key = pseudoRandomKey,
                        data = previous + info + byteArrayOf(counter.toByte()),
                    )
                val bytesToCopy = minOf(previous.size, outputLength - offset)
                previous.copyInto(output, destinationOffset = offset, endIndex = bytesToCopy)
                offset += bytesToCopy
                counter++
            }

            return output
        }

        private fun hmacSha256(
            key: ByteArray,
            data: ByteArray,
        ): ByteArray {
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
            return mac.doFinal(data)
        }

        private fun String.toAesKey(): SecretKeySpec = SecretKeySpec(fromBase64(), AES_ALGORITHM)

        private fun String.fromBase64(): ByteArray = Base64.getDecoder().decode(this)

        private fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

        private companion object {
            private const val AES_ALGORITHM = "AES"
            private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
            private const val AES_KEY_SIZE_BYTES = 32
            private const val GCM_IV_SIZE_BYTES = 12
            private const val GCM_TAG_SIZE_BITS = 128

            private const val EC_ALGORITHM = "EC"
            private const val ECDH_ALGORITHM = "ECDH"
            private const val CURVE_NAME = "secp256r1"
            private const val COORDINATE_SIZE_BYTES = 32
            private const val RAW_PUBLIC_KEY_SIZE_BYTES = 65
            private const val UNCOMPRESSED_POINT_PREFIX = 0x04.toByte()

            private const val HMAC_ALGORITHM = "HmacSHA256"
            private const val HKDF_HASH_SIZE_BYTES = 32
            private const val HKDF_INFO = "conversation-key-wrap"
        }
    }

data class EncryptedPayload(
    val iv: String,
    val ciphertext: String,
)
