package com.citruschat.citrusmobile.data.auth

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.citruschat.citrusmobile.core.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.getValue

@Singleton
class EncryptedPrefsTokenStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val logger: Logger,
    ) : TokenStore {
        private val prefs by lazy {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        private val tokensState = MutableStateFlow(loadTokens())

        override fun observeTokens(): Flow<AuthTokens?> = tokensState.asStateFlow()

        override suspend fun saveTokens(tokens: AuthTokens) {
            logger.i(TAG, "Saving auth tokens")
            prefs
                .edit {
                    putString(ACCESS_TOKEN_KEY, tokens.accessToken)
                        .putString(REFRESH_TOKEN_KEY, tokens.refreshToken)
                }

            tokensState.value = tokens
            logger.d(TAG, "Auth tokens saved")
        }

        override suspend fun clearTokens() {
            logger.i(TAG, "Clearing auth tokens")
            prefs
                .edit {
                    remove(ACCESS_TOKEN_KEY)
                        .remove(REFRESH_TOKEN_KEY)
                }

            tokensState.value = null
            logger.d(TAG, "Auth tokens cleared")
        }

        private fun loadTokens(): AuthTokens? {
            val accessToken = prefs.getString(ACCESS_TOKEN_KEY, null)
            val refreshToken = prefs.getString(REFRESH_TOKEN_KEY, null)
            if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
                logger.v(TAG, "No persisted tokens found")
                return null
            }

            logger.v(TAG, "Persisted tokens loaded")
            return AuthTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        }

        private companion object {
            private const val PREFS_NAME = "auth_secure_prefs"
            private const val ACCESS_TOKEN_KEY = "access_token"
            private const val REFRESH_TOKEN_KEY = "refresh_token"
            private const val TAG = "TokenStore"
        }
    }
