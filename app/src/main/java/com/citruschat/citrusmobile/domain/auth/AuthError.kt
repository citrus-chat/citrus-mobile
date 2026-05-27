package com.citruschat.citrusmobile.domain.auth

sealed interface AuthError {
    data class Http(
        val code: Int,
        val message: String? = null,
    ) : AuthError

    data object Network : AuthError

    data object Unknown : AuthError
}
