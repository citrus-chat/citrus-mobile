package com.citruschat.citrusmobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val username: String,
    val remoteProfilePictureUrl: String? = null,
    val localProfilePicturePath: String? = null,
    val statusMessage: String? = null,
    val createdAt: String = "",
    val isCurrentUser: Boolean = false,
)
