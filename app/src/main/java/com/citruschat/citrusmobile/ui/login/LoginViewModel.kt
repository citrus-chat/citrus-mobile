package com.citruschat.citrusmobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.mapper.toMessageRes
import com.citruschat.citrusmobile.domain.auth.AuthResult
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
        private val logger: Logger,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        fun onEmailChange(email: String) {
            _uiState.update { it.copy(email = email, errorMessage = null) }
        }

        fun onPasswordChange(password: String) {
            _uiState.update { it.copy(password = password, errorMessage = null) }
        }

        fun login() {
            val current = _uiState.value
            logger.i(TAG, "Login requested")

            if (current.email.isBlank() || current.password.isBlank()) {
                logger.w(TAG, "Login blocked due to blank credentials")
                _uiState.update { it.copy(errorMessageRes = R.string.auth_username_password_required) }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val result = authRepository.login(current.email, current.password)) {
                    is AuthResult.Success -> {
                        logger.i(TAG, "Login successful")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                errorMessageRes = null,
                                password = "",
                            )
                        }
                    }

                    is AuthResult.Error -> {
                        logger.w(TAG, "Login failed with ${result.error}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = false,
                                errorMessageRes = result.error.toMessageRes(),
                            )
                        }
                    }
                }
            }
        }
    }

private const val TAG = "LoginViewModel"
