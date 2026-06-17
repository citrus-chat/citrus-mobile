package com.citruschat.citrusmobile.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.citruschat.citrusmobile.core.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPrefsTokenStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val logger: Logger,
    ) : TokenStore {
        private val prefs: SharedPreferences by lazy {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        private val secretKey: SecretKey by lazy {
            getOrCreateSecretKey()
        }

        private val tokensState = MutableStateFlow(loadTokens())

        override fun observeTokens(): Flow<AuthTokens?> = tokensState.asStateFlow()

        override suspend fun saveTokens(tokens: AuthTokens) {
            logger.i(TAG, "Saving auth token")

            val encrypted = encrypt(tokens.accessToken)

            prefs
                .edit()
                .putString(ACCESS_TOKEN_CIPHERTEXT_KEY, encrypted.cipherText)
                .putString(ACCESS_TOKEN_IV_KEY, encrypted.iv)
                .apply()

            tokensState.value = tokens
            logger.d(TAG, "Auth token saved")
        }

        override suspend fun clearTokens() {
            logger.i(TAG, "Clearing auth token")

            prefs
                .edit()
                .remove(ACCESS_TOKEN_CIPHERTEXT_KEY)
                .remove(ACCESS_TOKEN_IV_KEY)
                .apply()

            tokensState.value = null
            logger.d(TAG, "Auth token cleared")
        }

        private fun loadTokens(): AuthTokens? {
            val cipherText = prefs.getString(ACCESS_TOKEN_CIPHERTEXT_KEY, null)
            val iv = prefs.getString(ACCESS_TOKEN_IV_KEY, null)

            if (cipherText.isNullOrBlank() || iv.isNullOrBlank()) {
                logger.v(TAG, "No persisted token found")
                return null
            }

            return try {
                val accessToken = decrypt(cipherText, iv)
                logger.v(TAG, "Persisted token loaded")
                AuthTokens(accessToken = accessToken)
            } catch (e: GeneralSecurityException) {
                logger.e(TAG, "Failed to load persisted token", e)
                null
            } catch (e: IllegalArgumentException) {
                logger.e(TAG, "Failed to load persisted token", e)
                null
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

            val keyGenerator =
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

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }

        private data class EncryptedValue(
            val cipherText: String,
            val iv: String,
        )

        private companion object {
            private const val PREFS_NAME = "auth_secure_prefs"
            private const val ACCESS_TOKEN_CIPHERTEXT_KEY = "access_token_ciphertext"
            private const val ACCESS_TOKEN_IV_KEY = "access_token_iv"

            private const val ANDROID_KEYSTORE = "AndroidKeyStore"
            private const val KEY_ALIAS = "auth_aes_key"

            private const val TRANSFORMATION = "AES/GCM/NoPadding"
            private const val GCM_TAG_SIZE = 128
            private const val AES_KEY_SIZE = 256

            private const val TAG = "TokenStore"
        }
    }
