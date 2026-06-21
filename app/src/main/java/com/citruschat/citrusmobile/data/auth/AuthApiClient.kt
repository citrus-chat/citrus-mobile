package com.citruschat.citrusmobile.data.auth

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.device.DeviceIdentity
import com.citruschat.citrusmobile.domain.auth.AuthError
import com.citruschat.citrusmobile.domain.auth.AuthResult
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
    ) : AuthRemoteDataSource {
        private val loginUrl = "${baseUrl.trimEnd('/')}/api/v1/auth/login"

        init {
            logger.i(TAG, "AuthApiClient initialized with URL: $loginUrl")
        }

        override suspend fun login(
            username: String,
            password: String,
            deviceIdentity: DeviceIdentity,
        ): AuthResult =
            withContext(Dispatchers.IO) {
                try {
                    logger.i(TAG, "Login request started")

                    val payload =
                        JSONObject()
                            .put("email", username)
                            .put("password", password)
                            .put("deviceRequest", deviceIdentity.toJson())
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
                                    message = AuthApiResponseParser.parseErrorMessage(body).ifBlank { null },
                                ),
                            )
                        }

                        logger.i(TAG, "Login request succeeded")
                        val parsed = AuthApiResponseParser.parseLoginResponse(body)
                        logger.d(TAG, parsed.toString())
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

        private fun DeviceIdentity.toJson(): JSONObject =
            JSONObject()
                .put("deviceId", deviceId)
                .put("deviceName", deviceName)
                .put("deviceType", deviceType)
                .put("publicKey", publicKey)

        private companion object {
            val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
            const val TAG = "AuthApiClient"
        }
    }
