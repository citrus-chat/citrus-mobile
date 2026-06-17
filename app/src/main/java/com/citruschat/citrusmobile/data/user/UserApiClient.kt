package com.citruschat.citrusmobile.data.user

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.auth.TokenStore
import com.citruschat.citrusmobile.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
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
        private val usersUrl = "${baseUrl.trimEnd('/')}/api/v1/users"

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
                    val requestBuilder = Request.Builder().url(url)
                    tokenStore.observeTokens().firstOrNull()?.accessToken?.takeIf { it.isNotBlank() }?.let { token ->
                        requestBuilder.header("Authorization", "Bearer $token")
                    }

                    okHttpClient.newCall(requestBuilder.get().build()).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            logger.w(TAG, "User search failed with code=${response.code}")
                            return@withContext emptyList()
                        }

                        UserApiResponseParser.parseUsers(body).also { users ->
                            logger.d(TAG, "User search returned count=${users.size}")
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

        private companion object {
            const val TAG = "UserApiClient"
        }
    }
