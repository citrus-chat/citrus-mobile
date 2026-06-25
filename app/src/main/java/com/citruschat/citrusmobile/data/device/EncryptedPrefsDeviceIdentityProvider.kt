package com.citruschat.citrusmobile.data.device

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.citruschat.citrusmobile.core.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPrefsDeviceIdentityProvider
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val logger: Logger,
    ) : DeviceIdentityProvider {
        private val prefs: SharedPreferences by lazy {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        private val secretKey: SecretKey by lazy {
            getOrCreateSecretKey()
        }

        private val keyGenerator = P256IdentityKeyGenerator()

        override suspend fun getOrCreateDeviceIdentity(): DeviceIdentity {
            loadDeviceIdentity()?.let { identity ->
                logger.d(TAG, "Existing device identity loaded")
                return identity
            }

            logger.i(TAG, "Creating mobile device identity")
            val keyPair = keyGenerator.generate()
            val identity =
                DeviceIdentity(
                    deviceId = UUID.randomUUID().toString(),
                    deviceName = deviceName(),
                    deviceType = DEVICE_TYPE_MOBILE,
                    publicKey = keyPair.publicKey,
                )
            val encryptedPrivateKey = encrypt(keyPair.privateKey)

            prefs
                .edit()
                .putString(DEVICE_ID_KEY, identity.deviceId)
                .putString(DEVICE_NAME_KEY, identity.deviceName)
                .putString(DEVICE_TYPE_KEY, identity.deviceType)
                .putString(PUBLIC_KEY_KEY, identity.publicKey)
                .putString(PRIVATE_KEY_CIPHERTEXT_KEY, encryptedPrivateKey.cipherText)
                .putString(PRIVATE_KEY_IV_KEY, encryptedPrivateKey.iv)
                .apply()

            return identity
        }

        override suspend fun clearDeviceIdentity() {
            logger.i(TAG, "Clearing local device identity")
            prefs
                .edit()
                .remove(DEVICE_ID_KEY)
                .remove(DEVICE_NAME_KEY)
                .remove(DEVICE_TYPE_KEY)
                .remove(PUBLIC_KEY_KEY)
                .remove(PRIVATE_KEY_CIPHERTEXT_KEY)
                .remove(PRIVATE_KEY_IV_KEY)
                .apply()
        }

        private fun loadDeviceIdentity(): DeviceIdentity? {
            val deviceId = prefs.getString(DEVICE_ID_KEY, null)
            val deviceName = prefs.getString(DEVICE_NAME_KEY, null)
            val deviceType = prefs.getString(DEVICE_TYPE_KEY, null)
            val publicKey = prefs.getString(PUBLIC_KEY_KEY, null)
            val encryptedPrivateKey = prefs.getString(PRIVATE_KEY_CIPHERTEXT_KEY, null)
            val privateKeyIv = prefs.getString(PRIVATE_KEY_IV_KEY, null)

            if (
                deviceId.isNullOrBlank() ||
                deviceName.isNullOrBlank() ||
                deviceType.isNullOrBlank() ||
                publicKey.isNullOrBlank() ||
                encryptedPrivateKey.isNullOrBlank() ||
                privateKeyIv.isNullOrBlank()
            ) {
                return null
            }

            return try {
                val privateKey = decrypt(encryptedPrivateKey, privateKeyIv)
                if (!hasExpectedPublicKeyFormat(publicKey)) {
                    logger.w(TAG, "Stored device identity has unsupported public key format")
                    return null
                }

                DeviceIdentity(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    deviceType = deviceType,
                    publicKey = publicKey,
                    privateKey = privateKey,
                )
            } catch (e: GeneralSecurityException) {
                logger.e(TAG, "Failed to load local device identity", e)
                null
            } catch (e: IllegalArgumentException) {
                logger.e(TAG, "Failed to load local device identity", e)
                null
            }
        }

        private fun hasExpectedPublicKeyFormat(publicKey: String): Boolean =
            runCatching {
                val decoded = Base64.decode(publicKey, Base64.NO_WRAP)
                decoded.size == P256IdentityKeyGenerator.RAW_PUBLIC_KEY_SIZE_BYTES &&
                    decoded.firstOrNull() == UNCOMPRESSED_POINT_PREFIX
            }.getOrDefault(false)

        private fun deviceName(): String {
            val manufacturer = Build.MANUFACTURER.trim()
            val model = Build.MODEL.trim()

            return when {
                manufacturer.isBlank() && model.isBlank() -> "Android Mobile"
                manufacturer.isBlank() -> model
                model.isBlank() -> manufacturer
                model.startsWith(manufacturer, ignoreCase = true) -> model
                else -> "$manufacturer $model"
            }
        }

        private fun encrypt(plainText: String): EncryptedValue {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            return EncryptedValue(
                cipherText = Base64.encodeToString(cipherBytes, Base64.NO_WRAP),
                iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            )
        }

        private fun decrypt(
            cipherText: String,
            iv: String,
        ): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_SIZE, Base64.decode(iv, Base64.NO_WRAP))

            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plainBytes = cipher.doFinal(Base64.decode(cipherText, Base64.NO_WRAP))
            return plainBytes.toString(Charsets.UTF_8)
        }

        private fun getOrCreateSecretKey(): SecretKey {
            val keyStore =
                KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                    load(null)
                }

            val existingKey = keyStore.getKey(KEY_ALIAS, null)
            if (existingKey is SecretKey) return existingKey

            val generator =
                KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE,
                )

            val spec =
                KeyGenParameterSpec
                    .Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(AES_KEY_SIZE)
                    .build()

            generator.init(spec)
            return generator.generateKey()
        }

        private data class EncryptedValue(
            val cipherText: String,
            val iv: String,
        )

        private companion object {
            private const val PREFS_NAME = "device_identity_secure_prefs"
            private const val DEVICE_ID_KEY = "device_id"
            private const val DEVICE_NAME_KEY = "device_name"
            private const val DEVICE_TYPE_KEY = "device_type"
            private const val PUBLIC_KEY_KEY = "public_key"
            private const val PRIVATE_KEY_CIPHERTEXT_KEY = "private_key_ciphertext"
            private const val PRIVATE_KEY_IV_KEY = "private_key_iv"
            private const val DEVICE_TYPE_MOBILE = "MOBILE"

            private const val ANDROID_KEYSTORE = "AndroidKeyStore"
            private const val KEY_ALIAS = "device_identity_aes_key"

            private const val TRANSFORMATION = "AES/GCM/NoPadding"
            private const val GCM_TAG_SIZE = 128
            private const val AES_KEY_SIZE = 256
            private const val UNCOMPRESSED_POINT_PREFIX = 0x04.toByte()

            private const val TAG = "DeviceIdentityProvider"
        }
    }
