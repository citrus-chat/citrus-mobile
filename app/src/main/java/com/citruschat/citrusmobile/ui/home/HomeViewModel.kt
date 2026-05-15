package com.citruschat.citrusmobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.domain.model.Chat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// to-do: Implement repository and inject it here
class HomeViewModel : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            HomeUiState(
                chats =
                    listOf(
                        Chat(id = 1, name = "Alice"),
                        Chat(id = 2, name = "Study Group"),
                        Chat(id = 3, name = "Bob"),
                    ),
            ),
        )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadChats() {
        viewModelScope.launch {
            // replace with repository call
        }
    }
}
