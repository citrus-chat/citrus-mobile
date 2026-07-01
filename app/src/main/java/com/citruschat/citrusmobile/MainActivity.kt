package com.citruschat.citrusmobile

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.citruschat.citrusmobile.app.AppVisibilityTracker
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
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by themeRepository.observeDarkTheme().collectAsState(initial = false)

            CitrusMobileTheme(darkTheme = isDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    val density = LocalDensity.current
                    val layoutDirection = LocalLayoutDirection.current
                    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    start = padding.calculateStartPadding(layoutDirection),
                                    top = padding.calculateTopPadding(),
                                    end = padding.calculateEndPadding(layoutDirection),
                                    bottom =
                                        if (isKeyboardOpen) {
                                            0.dp
                                        } else {
                                            padding.calculateBottomPadding()
                                        },
                                ),
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATIONS_CODE,
        )
    }
}

@HiltAndroidApp
class CitrusChat : Application() {
    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var appVisibilityTracker: AppVisibilityTracker

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(appVisibilityTracker)
        logger.i(APP_TAG, "Application initialized")
    }
}

private const val REQUEST_NOTIFICATIONS_CODE = 1001
