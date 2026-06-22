package com.citruschat.citrusmobile.data.user

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserApiClient
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val tokenStore: TokenStore,
        private val logger: Logger,
        baseUrl: String,
    ) : UserRemoteDataSource {
        private val apiBaseUrl = baseUrl.trimEnd('/')
        private val usersUrl = "$apiBaseUrl/api/v1/users"
        private val currentUserUrl = "$apiBaseUrl/api/v1/auth/me"
        private val currentUserAvatarUrl = "$apiBaseUrl/api/v1/users/me/avatar"
        private val userAvatarsUrl = "$apiBaseUrl/api/v1/users/avatars"

        init {
            logger.i(TAG, "UserApiClient initialized with URL: $usersUrl")
        }

        override suspend fun searchUsers(query: String): List<User> =
            withContext(Dispatchers.IO) {
                if (query.isBlank()) return@withContext emptyList()

                try {
                    val url =
                        usersUrl
                            .toHttpUrl()
                            .newBuilder()
                            .addQueryParameter("search", query)
                            .build()

                    okHttpClient.newCall(authorizedRequest(url.toString()).get().build()).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            logger.w(TAG, "User search failed with code=${response.code}")
                            return@withContext emptyList()
                        }

                        UserApiResponseParser.parseUsers(body).also { users ->
                            logger.d(TAG, "User search returned count=${users.size}")
                            users.forEach { user ->
                                logger.d(TAG, user.toString())
                            }
                        }
                    }
                } catch (t: IOException) {
                    logger.e(TAG, "User search network failure", t)
                    emptyList()
                } catch (t: JSONException) {
                    logger.e(TAG, "User search parse failure", t)
                    emptyList()
                } catch (t: IllegalArgumentException) {
                    logger.e(TAG, "User search URL failure", t)
                    emptyList()
                }
            }

        override suspend fun getCurrentUser(): User? =
            withContext(Dispatchers.IO) {
                try {
                    okHttpClient.newCall(authorizedRequest(currentUserUrl).get().build()).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            logger.w(TAG, "Current user request failed with code=${response.code}")
                            return@withContext null
                        }

                        UserApiResponseParser.parseCurrentUser(body)
                    }
                } catch (t: IOException) {
                    logger.e(TAG, "Current user network failure", t)
                    null
                } catch (t: JSONException) {
                    logger.e(TAG, "Current user parse failure", t)
                    null
                }
            }

        override suspend fun downloadAvatar(filename: String): ByteArray? =
            withContext(Dispatchers.IO) {
                if (filename.isBlank()) return@withContext null

                try {
                    val url =
                        userAvatarsUrl
                            .toHttpUrl()
                            .newBuilder()
                            .addPathSegment(filename)
                            .build()

                    okHttpClient.newCall(authorizedRequest(url.toString()).get().build()).execute().use { response ->
                        if (!response.isSuccessful) {
                            logger.w(TAG, "Avatar download failed with code=${response.code}")
                            return@withContext null
                        }

                        response.body?.bytes()
                    }
                } catch (t: IOException) {
                    logger.e(TAG, "Avatar download network failure", t)
                    null
                } catch (t: IllegalArgumentException) {
                    logger.e(TAG, "Avatar download URL failure", t)
                    null
                }
            }

        override suspend fun updateCurrentUserAvatar(
            bytes: ByteArray,
            fileName: String,
            mimeType: String,
        ): String? =
            withContext(Dispatchers.IO) {
                if (bytes.isEmpty()) return@withContext null

                try {
                    val requestBody =
                        MultipartBody
                            .Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart(
                                name = "file",
                                filename = fileName.ifBlank { DEFAULT_AVATAR_FILE_NAME },
                                body = bytes.toRequestBody(mimeType.toMediaType()),
                            ).build()

                    okHttpClient.newCall(authorizedRequest(currentUserAvatarUrl).put(requestBody).build()).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            logger.w(TAG, "Avatar upload failed with code=${response.code}")
                            return@withContext null
                        }

                        UserApiResponseParser.parseAvatarUrl(body)
                    }
                } catch (t: IOException) {
                    logger.e(TAG, "Avatar upload network failure", t)
                    null
                } catch (t: JSONException) {
                    logger.e(TAG, "Avatar upload parse failure", t)
                    null
                } catch (t: IllegalArgumentException) {
                    logger.e(TAG, "Avatar upload media type failure", t)
                    null
                }
            }

        private suspend fun authorizedRequest(url: String): Request.Builder {
            val requestBuilder = Request.Builder().url(url)
            tokenStore.observeTokens().firstOrNull()?.accessToken?.takeIf { it.isNotBlank() }?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }
            return requestBuilder
        }

        private companion object {
            const val DEFAULT_AVATAR_FILE_NAME = "avatar"
            const val TAG = "UserApiClient"
        }
    }
