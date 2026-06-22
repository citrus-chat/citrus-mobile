package com.citruschat.citrusmobile.ui.devices

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.citruschat.citrusmobile.R

@Composable
fun DeviceQrScannerScreen(
    onBackClick: () -> Unit,
    viewModel: ConnectedDevicesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasCameraPermission = isGranted
        }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colorResource(R.color.scanner_background)),
    ) {
        ScannerTopBar(onBackClick = onBackClick)
        ScannerHint()
        ScannerPreviewSection(
            hasCameraPermission = hasCameraPermission,
            onQrPayloadScanned = viewModel::onQrPayloadScanned,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ScannerTopBar(onBackClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .background(colorResource(R.color.scanner_chrome))
                .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.device_qr_scanner_back_content_description),
                tint = colorResource(R.color.white),
            )
        }
        Text(
            text = stringResource(R.string.device_qr_scanner_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorResource(R.color.white),
        )
    }
}

@Composable
private fun ScannerHint() {
    Text(
        text = stringResource(R.string.device_qr_scanner_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = colorResource(R.color.scanner_muted_text),
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.scanner_chrome))
                .padding(horizontal = 38.dp, vertical = 18.dp),
    )
}

@Composable
private fun ScannerPreviewSection(
    hasCameraPermission: Boolean,
    onQrPayloadScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colorResource(R.color.scanner_background)),
        contentAlignment = Alignment.Center,
    ) {
        if (hasCameraPermission) {
            CameraPreview(onQrPayloadScanned = onQrPayloadScanned)
        } else {
            Text(
                text = stringResource(R.string.device_qr_scanner_permission_required),
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(R.color.scanner_muted_text),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp),
            )
        }

        ScannerOverlay()
    }
}

@Composable
private fun CameraPreview(onQrPayloadScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    val previewView =
        remember(context) {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize(),
    )

    DisposableEffect(cameraProviderFuture, lifecycleOwner, previewView) {
        val executor = ContextCompat.getMainExecutor(context)
        val listener =
            Runnable {
                val cameraProvider = cameraProviderFuture.get()
                val preview =
                    Preview.Builder().build().also { cameraPreview ->
                        cameraPreview.surfaceProvider = previewView.surfaceProvider
                    }
                val analysis =
                    ImageAnalysis
                        .Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                // TODO: Decode QR frames and pass the payload to onQrPayloadScanned.
                                imageProxy.close()
                            }
                        }

                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }
            }

        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }
}

@Composable
private fun ScannerOverlay() {
    val scannerScrim = colorResource(R.color.scanner_scrim)
    val scannerWindowTint = colorResource(R.color.scanner_window_tint)
    val scannerFrameStroke = colorResource(R.color.scanner_frame_stroke)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val frameSize = size.minDimension * 0.74f
        val frameLeft = (size.width - frameSize) / 2f
        val frameTop = (size.height - frameSize) / 2f
        val frameCorner = 16.dp.toPx()
        val strokeWidth = 2.dp.toPx()

        drawRect(color = scannerScrim)
        drawRoundRect(
            color = scannerWindowTint,
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameSize, frameSize),
            cornerRadius = CornerRadius(frameCorner, frameCorner),
        )
        drawRoundRect(
            color = scannerFrameStroke,
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameSize, frameSize),
            cornerRadius = CornerRadius(frameCorner, frameCorner),
            style =
                Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.cornerPathEffect(frameCorner),
                ),
        )
    }
}
