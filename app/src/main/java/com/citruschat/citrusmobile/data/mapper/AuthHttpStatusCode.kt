package com.citruschat.citrusmobile.data.mapper

enum class AuthHttpStatusCode(
    val code: Int,
) {
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    ;

    companion object {
        fun from(code: Int): AuthHttpStatusCode? = entries.firstOrNull { it.code == code }
    }
}
