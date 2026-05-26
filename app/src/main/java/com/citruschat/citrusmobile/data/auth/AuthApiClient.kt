package com.citruschat.citrusmobile.data.auth

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

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
        ): AuthTokens {
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
                    val errorMessage = body.takeIf { it.isNotBlank() } ?: "Login failed"
                    throw IllegalStateException(errorMessage)
                }

                return parseTokens(body)
            }
        }

        private fun parseTokens(responseBody: String): AuthTokens {
            val json = JSONObject(responseBody)
            val accessToken =
                json.optString("access_token").takeIf { it.isNotBlank() }
                    ?: json.optString("accessToken").takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Missing access token")
            val refreshToken =
                json.optString("refresh_token").takeIf { it.isNotBlank() }
                    ?: json.optString("refreshToken").takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Missing refresh token")

            return AuthTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        }

        private companion object {
            val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        }
    }
