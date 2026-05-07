package com.citruschat.citrusmobile.ui.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        fun onUsernameChange(username: String) {
            _uiState.update { it.copy(username = username, errorMessage = null) }
        }

        fun onPasswordChange(password: String) {
            _uiState.update { it.copy(password = password, errorMessage = null) }
        }

        fun login() {
            val current = _uiState.value

            if (current.username.isBlank() && current.password.isBlank()) {
                _uiState.update { it.copy(isLoggedIn = true, errorMessage = null) }
                return
            }

            if (current.username.isBlank() || current.password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Username and password are required") }
                return
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Later: call API here
            _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
        }
    }
