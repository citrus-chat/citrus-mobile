package com.citruschat.citrusmobile.domain.auth

sealed interface AuthState {
    data object Loading : AuthState

    data object Authenticated : AuthState

    data object Unauthenticated : AuthState
}
