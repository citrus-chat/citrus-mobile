package com.citruschat.citrusmobile.data.auth

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Singleton
class AuthApiClient
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        baseUrl: String,
    ) {
        private val loginUrl = "${baseUrl.trimEnd('/')}/auth/login"

        suspend fun login(
            username: String,
            password: String,
        ): AuthTokens =
            withContext(Dispatchers.IO) {
                val payload =
                    JSONObject()
                        .put("username", username)
                        .put("password", password)
                        .toString()
                        .toRequestBody(JSON_MEDIA_TYPE)

                val request =
                    Request
                        .Builder()
                        .url(loginUrl)
                        .post(payload)
                        .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val errorMessage = parseErrorMessage(body)
                        throw AuthApiException(errorMessage)
                    }

                    return@withContext parseTokens(body)
                }
            }

        private fun parseTokens(responseBody: String): AuthTokens {
            val json = JSONObject(responseBody)
            val accessToken =
                json.optString("access_token").takeIf { it.isNotBlank() }
                    ?: json.optString("accessToken").takeIf { it.isNotBlank() }
                    ?: throw AuthApiException("Missing access token")
            val refreshToken =
                json.optString("refresh_token").takeIf { it.isNotBlank() }
                    ?: json.optString("refreshToken").takeIf { it.isNotBlank() }
                    ?: throw AuthApiException("Missing refresh token")

            return AuthTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        }

        private fun parseErrorMessage(responseBody: String): String {
            if (responseBody.isBlank()) return "Login failed"
            return runCatching {
                val json = JSONObject(responseBody)
                json.optString("message").takeIf { it.isNotBlank() }
                    ?: json.optString("error").takeIf { it.isNotBlank() }
                    ?: "Login failed"
            }.getOrDefault("Login failed")
        }

        private companion object {
            val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        }
    }

class AuthApiException(
    message: String,
) : IOException(message)
