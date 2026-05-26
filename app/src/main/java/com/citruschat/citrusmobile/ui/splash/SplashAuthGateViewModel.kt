package com.citruschat.citrusmobile.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.domain.model.AuthState
import com.citruschat.citrusmobile.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SplashAuthGateViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
    ) : ViewModel() {
        val authState: StateFlow<AuthState> =
            authRepository
                .observeAuthState()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = AuthState.Loading,
                )
    }
