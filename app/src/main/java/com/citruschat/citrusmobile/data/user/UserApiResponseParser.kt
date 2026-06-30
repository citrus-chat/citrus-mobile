package com.citruschat.citrusmobile.data.user

import com.citruschat.citrusmobile.domain.model.User
import com.citruschat.citrusmobile.domain.model.UserProfile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object UserApiResponseParser {
    fun parseUsers(responseBody: String): List<User> {
        if (responseBody.isBlank()) return emptyList()

        val users = JSONObject(responseBody).getJSONArray("data")
        return users.toUserList()
    }

    fun parseCurrentUser(responseBody: String): User {
        val user = JSONObject(responseBody).getJSONObject("data")
        return User(
            id = user.requiredString("userId"),
            email = user.requiredString("email"),
            username = user.requiredString("username"),
            remoteProfilePictureUrl = user.optionalString("avatar_url"),
            localProfilePicturePath = null,
            isCurrentUser = true,
        )
    }

    fun parseCurrentUserProfile(responseBody: String): UserProfile {
        val profile = JSONObject(responseBody).getJSONObject("data")
        return profile.toUserProfile()
    }

    fun parseAvatarUrl(responseBody: String): String? =
        JSONObject(responseBody)
            .getJSONObject("data")
            .optionalString("avatar_url")

    private fun JSONArray.toUserList(): List<User> =
        buildList {
            for (index in 0 until length()) {
                add(getJSONObject(index).toUser())
            }
        }

    private fun JSONObject.toUser(): User =
        User(
            id = requiredString("id"),
            email = requiredString("email"),
            username = requiredString("username"),
            remoteProfilePictureUrl = optionalString("avatar_url"),
            localProfilePicturePath = null,
        )

    private fun JSONObject.toUserProfile(): UserProfile =
        UserProfile(
            userId = requiredString("userId"),
            username = requiredString("username"),
            avatarUrl = optionalString("avatarUrl"),
            description = optionalString("description").orEmpty(),
            privacy = optionalString("privacy") ?: UserProfile.DEFAULT_PRIVACY,
            showPhone = optBoolean("showPhone", false),
            showEmail = optBoolean("showEmail", false),
            showStatus = optBoolean("showStatus", false),
            showDescription = optBoolean("showDescription", false),
            allowGroupInvites = optBoolean("allowGroupInvites", false),
        )

    private fun JSONObject.requiredString(name: String): String =
        optString(name)
            .takeIf { it.isNotBlank() && it != "null" }
            ?: throw JSONException("Missing $name")

    private fun JSONObject.optionalString(name: String): String? =
        optString(name)
            .takeIf { it.isNotBlank() && it != "null" }
}
