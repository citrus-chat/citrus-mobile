package com.citruschat.citrusmobile.ui.devices

data class ConnectedDeviceUiModel(
    val id: String,
    val name: String,
    val platform: String,
    val lastSeenText: String,
)
