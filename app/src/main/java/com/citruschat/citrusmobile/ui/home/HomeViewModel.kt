package com.citruschat.citrusmobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.core.logging.Logger
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
        private val logger: Logger,
    ) : ViewModel() {
        val uiState: StateFlow<HomeUiState> =
            repository
                .observeChatsItems()
                .map { chats ->
                    logger.d(TAG, "Chat list loaded with count=${chats.size}")
                    HomeUiState(
                        chats = chats,
                        isLoading = false,
                        errorMessage = null,
                    )
                }.onStart {
                    logger.i(TAG, "Chat list loading started")
                    emit(HomeUiState(isLoading = true))
                }
                .catch { t ->
                    logger.e(TAG, "Chat list loading failed", t)
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

private const val TAG = "HomeViewModel"
