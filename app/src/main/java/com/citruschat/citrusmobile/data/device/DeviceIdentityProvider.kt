package com.citruschat.citrusmobile.data.device

interface DeviceIdentityProvider {
    suspend fun getOrCreateDeviceIdentity(): DeviceIdentity

    suspend fun clearDeviceIdentity()
}
