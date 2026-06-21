package com.citruschat.citrusmobile.data.device

data class DeviceIdentity(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val publicKey: String,
)
