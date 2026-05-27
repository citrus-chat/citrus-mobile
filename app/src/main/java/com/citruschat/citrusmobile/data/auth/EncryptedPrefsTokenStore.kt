package com.citruschat.citrusmobile.data.auth

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
            prefs
                .edit {
                    putString(ACCESS_TOKEN_KEY, tokens.accessToken)
                        .putString(REFRESH_TOKEN_KEY, tokens.refreshToken)
                }

            tokensState.value = tokens
        }

        override suspend fun clearTokens() {
            prefs
                .edit {
                    remove(ACCESS_TOKEN_KEY)
                        .remove(REFRESH_TOKEN_KEY)
                }

            tokensState.value = null
        }

        private fun loadTokens(): AuthTokens? {
            val accessToken = prefs.getString(ACCESS_TOKEN_KEY, null)
            val refreshToken = prefs.getString(REFRESH_TOKEN_KEY, null)
            if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) return null

            return AuthTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        }

        private companion object {
            private const val PREFS_NAME = "auth_secure_prefs"
            private const val ACCESS_TOKEN_KEY = "access_token"
            private const val REFRESH_TOKEN_KEY = "refresh_token"
        }
    }
