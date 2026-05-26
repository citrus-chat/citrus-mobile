package com.citruschat.citrusmobile.domain.model

sealed interface AuthState {
    data object Loading : AuthState

    data object Authenticated : AuthState

    data object Unauthenticated : AuthState
}
