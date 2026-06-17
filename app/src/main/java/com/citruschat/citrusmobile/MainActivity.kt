package com.citruschat.citrusmobile

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.domain.repository.ThemeRepository
import com.citruschat.citrusmobile.navigation.AppNavHost
import com.citruschat.citrusmobile.ui.theme.CitrusMobileTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

private const val TAG = "MainActivity"
private const val APP_TAG = "CitrusChatApp"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var themeRepository: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.i(TAG, "onCreate")
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by themeRepository.observeDarkTheme().collectAsState(initial = false)

            CitrusMobileTheme(darkTheme = isDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(padding),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        AppNavHost(logger = logger)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        logger.i(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        logger.v(TAG, "onResume")
    }

    override fun onStop() {
        logger.v(TAG, "onStop")
        super.onStop()
    }
}

@HiltAndroidApp
class CitrusChat : Application() {
    @Inject
    lateinit var logger: Logger

    override fun onCreate() {
        super.onCreate()
        logger.i(APP_TAG, "Application initialized")
    }
}
