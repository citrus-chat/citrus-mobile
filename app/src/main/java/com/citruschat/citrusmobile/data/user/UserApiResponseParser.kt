package com.citruschat.citrusmobile.data.user

import com.citruschat.citrusmobile.domain.model.User
import org.json.JSONArray
import org.json.JSONObject

object UserApiResponseParser {
    fun parseUsers(responseBody: String): List<User> {
        if (responseBody.isBlank()) return emptyList()

        val trimmed = responseBody.trim()
        val array =
            if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                JSONObject(trimmed).usersArray()
            }

        return buildList {
            for (index in 0 until array.length()) {
                when (val value = array.get(index)) {
                    is JSONObject -> value.toUser()?.let(::add)
                    is String -> add(value.toUser())
                }
            }
        }.distinctBy { it.id }
    }

    private fun JSONObject.usersArray(): JSONArray =
        optJSONArray("data")
            ?: optJSONArray("users")
            ?: optJSONArray("results")
            ?: optJSONArray("items")
            ?: optJSONObject("data")?.usersArray()
            ?: JSONArray()

    private fun JSONObject.toUser(): User? {
        val username = usernameOrNull()
        val email = emailOrNull()
        val id = idOrNull(username, email) ?: return null

        return User(
            id = id,
            email = email.orEmpty(),
            username = username ?: email?.substringBefore('@') ?: id,
            profilePictureUrl = profilePictureUrlOrNull(),
            statusMessage = statusMessageOrNull(),
            createdAt = stringOrNull("createdAt").orEmpty(),
        )
    }

    private fun JSONObject.usernameOrNull(): String? =
        stringOrNull("username")
            ?: stringOrNull("name")
            ?: stringOrNull("displayName")

    private fun JSONObject.emailOrNull(): String? = stringOrNull("email") ?: stringOrNull("mail")

    private fun JSONObject.idOrNull(
        username: String?,
        email: String?,
    ): String? =
        stringOrNull("id")
            ?: stringOrNull("userId")
            ?: stringOrNull("_id")
            ?: username
            ?: email

    private fun JSONObject.profilePictureUrlOrNull(): String? =
        stringOrNull("profilePictureUrl")
            ?: stringOrNull("profilePicture")
            ?: stringOrNull("avatarUrl")

    private fun JSONObject.statusMessageOrNull(): String? =
        stringOrNull("statusMessage")
            ?: stringOrNull("status")
            ?: stringOrNull("bio")

    private fun String.toUser(): User =
        User(
            id = this,
            email = "",
            username = this,
        )

    private fun JSONObject.stringOrNull(name: String): String? =
        optString(name)
            .takeIf { it.isNotBlank() && it != "null" }
}
