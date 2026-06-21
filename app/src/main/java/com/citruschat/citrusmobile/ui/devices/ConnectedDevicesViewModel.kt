package com.citruschat.citrusmobile.ui.devices

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ConnectedDevicesViewModel
    @Inject
    constructor() : ViewModel() {
        private val _devices = MutableStateFlow<List<ConnectedDeviceUiModel>>(emptyList())
        val devices: StateFlow<List<ConnectedDeviceUiModel>> = _devices.asStateFlow()

        init {
            // TODO: Load connected devices from local storage and sync them through REST/websocket
            // when the API exists.
        }

        fun onQrPayloadScanned(payload: String) {
            // TODO: Call the API to link the web device with payload, then refresh local storage.
        }
    }
