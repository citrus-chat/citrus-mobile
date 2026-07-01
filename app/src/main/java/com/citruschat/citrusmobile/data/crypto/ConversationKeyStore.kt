package com.citruschat.citrusmobile.data.crypto

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationKeyStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun saveKey(
            conversationId: String,
            keyVersion: Int,
            key: String,
        ) {
            prefs
                .edit()
                .putString(storageKey(conversationId, keyVersion), key)
                .putInt(activeVersionKey(conversationId), keyVersion)
                .apply()
        }

        fun getActiveKey(conversationId: String): ConversationKey? {
            val version = prefs.getInt(activeVersionKey(conversationId), NO_ACTIVE_VERSION)
            if (version == NO_ACTIVE_VERSION) return null
            val key = prefs.getString(storageKey(conversationId, version), null) ?: return null
            return ConversationKey(conversationId = conversationId, keyVersion = version, key = key)
        }

        fun getKey(
            conversationId: String,
            keyVersion: Int,
        ): ConversationKey? {
            val key = prefs.getString(storageKey(conversationId, keyVersion), null) ?: return null
            return ConversationKey(conversationId = conversationId, keyVersion = keyVersion, key = key)
        }

        private fun storageKey(
            conversationId: String,
            keyVersion: Int,
        ): String = "key:$conversationId:$keyVersion"

        private fun activeVersionKey(conversationId: String): String = "active:$conversationId"

        private companion object {
            const val PREFS_NAME = "conversation_key_store"
            const val NO_ACTIVE_VERSION = -1
        }
    }

data class ConversationKey(
    val conversationId: String,
    val keyVersion: Int,
    val key: String,
)
