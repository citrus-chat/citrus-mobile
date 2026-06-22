package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.data.local.entity.UserEntity
import com.citruschat.citrusmobile.domain.model.User

fun UserEntity.toDomain() =
    User(
        id = id,
        email = email,
        username = username,
        localProfilePicturePath = localProfilePicturePath,
        remoteProfilePictureUrl = remoteProfilePictureUrl,
        statusMessage = statusMessage,
        createdAt = createdAt,
        isCurrentUser = isCurrentUser,
    )

fun User.toEntity() =
    UserEntity(
        id = id,
        email = email,
        username = username,
        localProfilePicturePath = localProfilePicturePath,
        remoteProfilePictureUrl = remoteProfilePictureUrl,
        statusMessage = statusMessage,
        createdAt = createdAt,
        isCurrentUser = isCurrentUser,
    )
