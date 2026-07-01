package com.citruschat.citrusmobile.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citruschat.citrusmobile.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        messageRepository: MessageRepository,
    ) : ViewModel() {
        val unreadCount: StateFlow<Int> =
            messageRepository
                .observeTotalUnreadCount()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = 0,
                )
    }
