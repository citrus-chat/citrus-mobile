package com.citruschat.citrusmobile.data.crypto

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
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationCrypto
    @Inject
    constructor() {
        private val secureRandom = SecureRandom()
        private val base64 = Base64.getEncoder()
        private val decoder = Base64.getDecoder()

        fun generateConversationKey(): String {
            val key = ByteArray(AES_KEY_SIZE_BYTES)
            secureRandom.nextBytes(key)
            return base64.encodeToString(key)
        }

        fun encryptMessage(
            plaintext: String,
            conversationKey: String,
        ): EncryptedPayload = encryptAesGcm(plaintext, decoder.decode(conversationKey))

        fun decryptMessage(
            payload: EncryptedPayload,
            conversationKey: String,
        ): String = decryptAesGcm(payload, decoder.decode(conversationKey))

        fun encryptConversationKeyForDevice(
            conversationKey: String,
            targetPublicKey: String,
            senderPrivateKey: String,
        ): EncryptedPayload =
            encryptAesGcm(
                plaintext = conversationKey,
                keyBytes = deriveConversationWrapKey(senderPrivateKey, targetPublicKey),
            )

        fun decryptConversationKeyFromDevice(
            payload: EncryptedPayload,
            senderPublicKey: String,
            targetPrivateKey: String,
        ): String =
            decryptAesGcm(
                payload = payload,
                keyBytes = deriveConversationWrapKey(targetPrivateKey, senderPublicKey),
            )

        private fun encryptAesGcm(
            plaintext: String,
            keyBytes: ByteArray,
        ): EncryptedPayload {
            val iv = ByteArray(GCM_IV_SIZE_BYTES)
            secureRandom.nextBytes(iv)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, AES), GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            return EncryptedPayload(
                iv = base64.encodeToString(iv),
                ciphertext = base64.encodeToString(encrypted),
            )
        }

        private fun decryptAesGcm(
            payload: EncryptedPayload,
            keyBytes: ByteArray,
        ): String {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, AES),
                GCMParameterSpec(GCM_TAG_SIZE_BITS, decoder.decode(payload.iv)),
            )
            return cipher.doFinal(decoder.decode(payload.ciphertext)).toString(Charsets.UTF_8)
        }

        private fun deriveConversationWrapKey(
            privateKey: String,
            publicKey: String,
        ): ByteArray {
            val keyFactory = KeyFactory.getInstance(EC)
            val private = keyFactory.generatePrivate(PKCS8EncodedKeySpec(decoder.decode(privateKey)))
            val public = keyFactory.generatePublic(rawPublicKeySpec(decoder.decode(publicKey)))
            val agreement = KeyAgreement.getInstance(ECDH)
            agreement.init(private)
            agreement.doPhase(public, true)
            val sharedSecret = agreement.generateSecret()
            return hkdfSha256(sharedSecret, WRAP_INFO.toByteArray(Charsets.UTF_8), AES_KEY_SIZE_BYTES)
        }

        private fun rawPublicKeySpec(rawPublicKey: ByteArray): ECPublicKeySpec {
            require(rawPublicKey.size == RAW_PUBLIC_KEY_SIZE_BYTES && rawPublicKey[0] == UNCOMPRESSED_POINT_PREFIX) {
                "Unsupported P-256 public key format"
            }
            val spec = p256Spec()
            val x = rawPublicKey.copyOfRange(1, 1 + EC_COORDINATE_SIZE_BYTES).toPositiveBigInteger()
            val y = rawPublicKey.copyOfRange(1 + EC_COORDINATE_SIZE_BYTES, RAW_PUBLIC_KEY_SIZE_BYTES).toPositiveBigInteger()
            return ECPublicKeySpec(ECPoint(x, y), spec)
        }

        private fun p256Spec(): ECParameterSpec {
            val parameters = AlgorithmParameters.getInstance(EC)
            parameters.init(ECGenParameterSpec(P256_CURVE))
            return parameters.getParameterSpec(ECParameterSpec::class.java)
        }

        private fun hkdfSha256(
            inputKeyMaterial: ByteArray,
            info: ByteArray,
            length: Int,
        ): ByteArray {
            val pseudoRandomKey = hmacSha256(ByteArray(HKDF_HASH_SIZE_BYTES), inputKeyMaterial)
            val output = ByteArray(length)
            var previous = ByteArray(0)
            var offset = 0
            var counter = 1
            while (offset < length) {
                val input = previous + info + counter.toByte()
                previous = hmacSha256(pseudoRandomKey, input)
                val bytesToCopy = minOf(previous.size, length - offset)
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
            val mac = Mac.getInstance(HMAC_SHA256)
            mac.init(SecretKeySpec(key, HMAC_SHA256))
            return mac.doFinal(data)
        }

        private fun ByteArray.toPositiveBigInteger(): java.math.BigInteger = java.math.BigInteger(1, this)

        private companion object {
            const val AES = "AES"
            const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
            const val EC = "EC"
            const val ECDH = "ECDH"
            const val HMAC_SHA256 = "HmacSHA256"
            const val P256_CURVE = "secp256r1"
            const val WRAP_INFO = "conversation-key-wrap"
            const val AES_KEY_SIZE_BYTES = 32
            const val HKDF_HASH_SIZE_BYTES = 32
            const val GCM_IV_SIZE_BYTES = 12
            const val GCM_TAG_SIZE_BITS = 128
            const val RAW_PUBLIC_KEY_SIZE_BYTES = 65
            const val EC_COORDINATE_SIZE_BYTES = 32
            const val UNCOMPRESSED_POINT_PREFIX = 0x04.toByte()
        }
    }

data class EncryptedPayload(
    val iv: String,
    val ciphertext: String,
)
