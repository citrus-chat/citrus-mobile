package com.citruschat.citrusmobile.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.domain.auth.AuthState
import com.citruschat.citrusmobile.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SplashAuthGateViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        logger: Logger,
    ) : ViewModel() {
        val authState: StateFlow<AuthState> =
            authRepository
                .observeAuthState()
                .onEach { state ->
                    logger.i(TAG, "Auth gate observed state=$state")
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = AuthState.Loading,
                )
    }

private const val TAG = "SplashAuthGateVM"
