package com.citruschat.citrusmobile.data.auth

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.domain.auth.AuthError
import com.citruschat.citrusmobile.domain.auth.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthApiClient
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val logger: Logger,
        baseUrl: String,
    ) {
        private val loginUrl = "${baseUrl.trimEnd('/')}/auth/login"

        init {
            logger.i(TAG, "AuthApiClient initialized with URL: ${loginUrl}")
        }

        suspend fun login(
            username: String,
            password: String,
        ): AuthResult =
            withContext(Dispatchers.IO) {
                try {
                    logger.i(TAG, "Login request started")
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
                            logger.w(TAG, "Login request failed with code=${response.code}")
                            return@withContext AuthResult.Error(
                                AuthError.Http(
                                    code = response.code,
                                    message = parseErrorMessage(body).ifBlank { null },
                                ),
                            )
                        }

                        logger.i(TAG, "Login request succeeded")
                        AuthResult.Success(parseTokens(body))
                    }
                } catch (t: IOException) {
                    logger.e(TAG, "Login request network failure", t)
                    AuthResult.Error(AuthError.Network)
                } catch (t: Exception) {
                    logger.e(TAG, "Login request unknown failure", t)
                    AuthResult.Error(AuthError.Unknown)
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
            const val TAG = "AuthApiClient"
        }
    }

class AuthApiException(
    message: String,
) : IOException(message)
