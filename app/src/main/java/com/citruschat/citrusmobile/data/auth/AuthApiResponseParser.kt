package com.citruschat.citrusmobile.data.auth

import com.citruschat.citrusmobile.domain.model.User
import org.json.JSONException
import org.json.JSONObject

object AuthApiResponseParser {
    fun parseLoginResponse(responseBody: String): ParsedLoginResponse {
        val data = JSONObject(responseBody).getJSONObject("data")
        val accessToken = data.requiredString("accessToken")
        val email = data.requiredString("email")
        val userId = data.requiredString("userId")
        val username = data.requiredString("username")

        return ParsedLoginResponse(
            tokens = AuthTokens(accessToken = accessToken),
            user =
                User(
                    id = userId,
                    email = email,
                    username = username,
                ),
        )
    }

    fun parseErrorMessage(responseBody: String): String {
        if (responseBody.isBlank()) return LOGIN_FAILED_MESSAGE

        return runCatching {
            JSONObject(responseBody)
                .optString("message")
                .takeIf { it.isNotBlank() }
                ?: LOGIN_FAILED_MESSAGE
        }.getOrDefault(LOGIN_FAILED_MESSAGE)
    }

    private fun JSONObject.requiredString(name: String): String =
        optString(name)
            .takeIf { it.isNotBlank() && it != "null" }
            ?: throw JSONException("Missing $name")

    private const val LOGIN_FAILED_MESSAGE = "Login failed"
}

data class ParsedLoginResponse(
    val tokens: AuthTokens,
    val user: User,
)
