package com.citruschat.citrusmobile.data.repository

import android.content.Context
import com.citruschat.citrusmobile.domain.repository.ThemeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : ThemeRepository {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        private val darkThemeState = MutableStateFlow(prefs.getBoolean(DARK_THEME_KEY, false))

        override fun observeDarkTheme(): Flow<Boolean> = darkThemeState.asStateFlow()

        override suspend fun setDarkTheme(enabled: Boolean) {
            prefs.edit().putBoolean(DARK_THEME_KEY, enabled).apply()
            darkThemeState.value = enabled
        }

        private companion object {
            private const val PREFS_NAME = "app_settings"
            private const val DARK_THEME_KEY = "dark_theme"
        }
    }
