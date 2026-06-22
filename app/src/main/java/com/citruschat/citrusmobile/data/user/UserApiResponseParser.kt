package com.citruschat.citrusmobile.data.user

import com.citruschat.citrusmobile.domain.model.User
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

    private fun JSONObject.requiredString(name: String): String =
        optString(name)
            .takeIf { it.isNotBlank() && it != "null" }
            ?: throw JSONException("Missing $name")

    private fun JSONObject.optionalString(name: String): String? =
        optString(name)
            .takeIf { it.isNotBlank() && it != "null" }
}
