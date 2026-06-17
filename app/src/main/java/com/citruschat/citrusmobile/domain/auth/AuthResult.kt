package com.citruschat.citrusmobile.domain.auth

import com.citruschat.citrusmobile.data.auth.AuthTokens
import com.citruschat.citrusmobile.domain.model.User

sealed interface AuthResult {
    data class Success(
        val tokens: AuthTokens,
        val user: User? = null,
    ) : AuthResult

    data class Error(
        val error: AuthError,
    ) : AuthResult
}
