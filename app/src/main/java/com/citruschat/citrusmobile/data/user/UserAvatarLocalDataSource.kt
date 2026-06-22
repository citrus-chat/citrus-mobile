package com.citruschat.citrusmobile.data.user

interface UserAvatarLocalDataSource {
    fun cachedPathFor(avatarUrl: String): String?

    fun saveAvatar(
        avatarUrl: String,
        bytes: ByteArray,
    ): String?
}
