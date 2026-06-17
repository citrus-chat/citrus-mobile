package com.citruschat.citrusmobile.data.auth

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.domain.auth.AuthError
import com.citruschat.citrusmobile.domain.auth.AuthResult
import com.citruschat.citrusmobile.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
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
        private val loginUrl = "${baseUrl.trimEnd('/')}/api/v1/auth/login"

        init {
            logger.i(TAG, "AuthApiClient initialized with URL: $loginUrl")
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
                            .put("email", username)
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
                        val parsed = parseLoginResponse(body)
                        AuthResult.Success(
                            tokens = parsed.tokens,
                            user = parsed.user,
                        )
                    }
                } catch (t: IOException) {
                    logger.e(TAG, "Login request network failure", t)
                    AuthResult.Error(AuthError.Network)
                } catch (t: JSONException) {
                    logger.e(TAG, "Login request parse failure", t)
                    AuthResult.Error(AuthError.Unknown)
                }
            }

        private fun parseLoginResponse(responseBody: String): ParsedLoginResponse {
            val json = JSONObject(responseBody)
            val dataObject = json.optJSONObject("data")

            return ParsedLoginResponse(
                tokens = AuthTokens(accessToken = json.accessTokenFrom(dataObject)),
                user = json.userObjectFrom(dataObject)?.toUser(),
            )
        }

        private fun JSONObject.userObjectFrom(dataObject: JSONObject?): JSONObject? =
            dataObject?.optJSONObject("user")
                ?: dataObject?.optJSONObject("currentUser")
                ?: dataObject?.takeIf { it.hasEmail() }
                ?: optJSONObject("user")
                ?: optJSONObject("currentUser")
                ?: takeIf { it.hasEmail() }

        private fun JSONObject.accessTokenFrom(dataObject: JSONObject?): String =
            when {
                optString("data").isNotBlank() && dataObject == null -> optString("data")
                else ->
                    dataObject?.stringOrNull("accessToken")
                        ?: dataObject?.stringOrNull("token")
                        ?: stringOrNull("accessToken")
                        ?: stringOrNull("token")
                        ?: throw AuthApiException("Missing access token")
            }

        private fun JSONObject.hasEmail(): Boolean = has("email") || has("mail")

        private fun JSONObject.toUser(): User? {
            val email = stringOrNull("email") ?: stringOrNull("mail") ?: return null
            val id =
                stringOrNull("id")
                    ?: stringOrNull("userId")
                    ?: stringOrNull("_id")
                    ?: email

            return User(
                id = id,
                email = email,
                username =
                    stringOrNull("username")
                        ?: stringOrNull("name")
                        ?: email.substringBefore('@'),
                profilePictureUrl =
                    stringOrNull("profilePictureUrl")
                        ?: stringOrNull("profilePicture")
                        ?: stringOrNull("avatarUrl"),
                statusMessage =
                    stringOrNull("statusMessage")
                        ?: stringOrNull("status")
                        ?: stringOrNull("bio"),
                createdAt = stringOrNull("createdAt").orEmpty(),
            )
        }

        private fun JSONObject.stringOrNull(name: String): String? =
            optString(name)
                .takeIf { it.isNotBlank() && it != "null" }

        private fun parseErrorMessage(responseBody: String): String {
            if (responseBody.isBlank()) return "Login failed"

            return runCatching {
                val json = JSONObject(responseBody)
                json.optString("message").takeIf { it.isNotBlank() }
                    ?: json.optString("error").takeIf { it.isNotBlank() }
                    ?: "Login failed"
            }.getOrDefault("Login failed")
        }

        private data class ParsedLoginResponse(
            val tokens: AuthTokens,
            val user: User?,
        )

        private companion object {
            val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
            const val TAG = "AuthApiClient"
        }
    }

class AuthApiException(
    message: String,
) : IOException(message)
