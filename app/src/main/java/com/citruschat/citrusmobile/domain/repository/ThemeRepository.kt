package com.citruschat.citrusmobile.domain.repository

import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun observeDarkTheme(): Flow<Boolean>

    suspend fun setDarkTheme(enabled: Boolean)
}
