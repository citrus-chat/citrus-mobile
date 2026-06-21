package com.citruschat.citrusmobile.data.auth

import com.citruschat.citrusmobile.data.device.DeviceIdentity
import com.citruschat.citrusmobile.domain.auth.AuthResult

interface AuthRemoteDataSource {
    suspend fun login(
        username: String,
        password: String,
        deviceIdentity: DeviceIdentity,
    ): AuthResult
}
