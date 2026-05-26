package com.citruschat.citrusmobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val repository: ChatRepository,
    ) : ViewModel() {
        val uiState: StateFlow<HomeUiState> =
            repository
                .observeChatsItems()
                .map { chats ->
                    HomeUiState(
                        chats = chats,
                        isLoading = false,
                        errorMessage = null,
                    )
                }.onStart { emit(HomeUiState(isLoading = true)) }
                .catch { t ->
                    emit(
                        HomeUiState(
                            chats = emptyList(),
                            isLoading = false,
                            errorMessage = t.message ?: "Failed to load chats",
                        ),
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = HomeUiState(isLoading = true),
                )
    }
