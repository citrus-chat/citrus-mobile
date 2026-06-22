package com.citruschat.citrusmobile.data.user

import android.content.Context
import com.citruschat.citrusmobile.core.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileUserAvatarLocalDataSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val logger: Logger,
    ) : UserAvatarLocalDataSource {
        private val avatarDirectory: File by lazy {
            File(context.filesDir, AVATAR_DIRECTORY).also { directory ->
                if (!directory.exists()) directory.mkdirs()
            }
        }

        override fun cachedPathFor(avatarUrl: String): String? {
            val file = avatarUrl.cacheFile()
            return file.takeIf { it.exists() && it.length() > 0L }?.absolutePath
        }

        override fun saveAvatar(
            avatarUrl: String,
            bytes: ByteArray,
        ): String? =
            runCatching {
                val file = avatarUrl.cacheFile()
                file.writeBytes(bytes)
                file.absolutePath
            }.onFailure { throwable ->
                logger.e(TAG, "Could not save avatar file", throwable)
            }.getOrNull()

        private fun String.cacheFile(): File = File(avatarDirectory, "${sha256()}.img")

        private fun String.sha256(): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
            return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
        }

        private companion object {
            const val AVATAR_DIRECTORY = "user_avatars"
            const val TAG = "FileUserAvatarLocalDataSource"
        }
    }
