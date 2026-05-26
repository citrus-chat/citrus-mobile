package com.citruschat.citrusmobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.domain.model.AuthState
import com.citruschat.citrusmobile.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                authRepository.observeAuthState().collect { authState ->
                    if (authState == AuthState.Authenticated) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                errorMessage = null,
                                password = "",
                            )
                        }
                    }
                }
            }
        }

        fun onUsernameChange(username: String) {
            _uiState.update { it.copy(username = username, errorMessage = null) }
        }

        fun onPasswordChange(password: String) {
            _uiState.update { it.copy(password = password, errorMessage = null) }
        }

        fun login() {
            val current = _uiState.value

            if (current.username.isBlank() || current.password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Username and password are required") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                authRepository
                    .login(username = current.username, password = current.password)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                errorMessage = null,
                                password = "",
                            )
                        }
                    }.onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = false,
                                errorMessage = throwable.message ?: "Login failed",
                            )
                        }
                    }
            }
        }
    }
