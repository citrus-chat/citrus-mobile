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
            profilePictureUrl = optionalString("avatar_url"),
        )

    private fun JSONObject.requiredString(name: String): String =
        optString(name)
            .takeIf { it.isNotBlank() && it != "null" }
            ?: throw JSONException("Missing $name")

    private fun JSONObject.optionalString(name: String): String? =
        optString(name)
            .takeIf { it.isNotBlank() && it != "null" }
}
