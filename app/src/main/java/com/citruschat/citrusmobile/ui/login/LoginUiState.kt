package com.citruschat.citrusmobile.ui.login

import androidx.annotation.StringRes

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
)
