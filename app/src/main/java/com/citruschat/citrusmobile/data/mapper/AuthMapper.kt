package com.citruschat.citrusmobile.data.mapper

import androidx.annotation.StringRes
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.auth.AuthError

@StringRes
fun AuthError.toMessageRes(): Int =
    when (this) {
        is AuthError.Http ->
            when (AuthHttpStatusCode.from(code)) {
                AuthHttpStatusCode.UNAUTHORIZED -> R.string.auth_invalid_credentials
                AuthHttpStatusCode.FORBIDDEN -> R.string.auth_access_denied
                else -> R.string.auth_login_failed
            }
        AuthError.Network -> R.string.auth_network_error
        AuthError.Unknown -> R.string.auth_login_failed
    }
