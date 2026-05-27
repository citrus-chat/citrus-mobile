package com.citruschat.citrusmobile.domain.auth

import com.citruschat.citrusmobile.data.auth.AuthTokens

sealed interface AuthResult {
    data class Success(
        val tokens: AuthTokens,
    ) : AuthResult

    data class Error(
        val error: AuthError,
    ) : AuthResult
}
