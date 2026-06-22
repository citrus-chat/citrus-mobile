package com.citruschat.citrusmobile.ui.profile

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citruschat.citrusmobile.ui.profile.component.ProfileHeader
import com.citruschat.citrusmobile.ui.profile.component.ProfileOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

@Composable
fun ProfileScreen(
    onLogoutComplete: () -> Unit,
    onConnectedDevicesClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val avatarPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            coroutineScope.launch {
                val selectedAvatar =
                    withContext(Dispatchers.IO) {
                        context.contentResolver.readSelectedAvatar(uri)
                    } ?: return@launch

                viewModel.uploadAvatar(
                    bytes = selectedAvatar.bytes,
                    fileName = selectedAvatar.fileName,
                    mimeType = selectedAvatar.mimeType,
                )
            }
        }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) onLogoutComplete()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        ProfileHeader(
            user = uiState.user,
            avatarLocalPath = uiState.avatarLocalPath,
            isAvatarUploading = uiState.isAvatarUploading,
            onAvatarClick = { avatarPicker.launch("image/*") },
        )

        Spacer(modifier = Modifier.weight(1f))

        ProfileOptions(
            isDarkTheme = uiState.isDarkTheme,
            isLoggingOut = uiState.isLoggingOut,
            onDarkThemeChange = viewModel::setDarkTheme,
            onConnectedDevicesClick = onConnectedDevicesClick,
            onLogoutClick = viewModel::logout,
        )
    }
}

private data class SelectedAvatar(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
)

private fun ContentResolver.readSelectedAvatar(uri: Uri): SelectedAvatar? {
    val bytes = openInputStream(uri)?.use { inputStream -> inputStream.readBytes() } ?: return null
    val fileName = uri.displayName(this) ?: DEFAULT_AVATAR_FILE_NAME
    val mimeType = getType(uri) ?: DEFAULT_AVATAR_MIME_TYPE

    if (bytes.size <= MAX_AVATAR_UPLOAD_BYTES) {
        return SelectedAvatar(
            bytes = bytes,
            fileName = fileName,
            mimeType = mimeType,
        )
    }

    val compressedBytes = compressAvatar(uri) ?: return null
    return SelectedAvatar(
        bytes = compressedBytes,
        fileName = fileName.toJpegFileName(),
        mimeType = JPEG_MIME_TYPE,
    )
}

private fun ContentResolver.compressAvatar(uri: Uri): ByteArray? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, boundsOptions)
    }

    val sourceWidth = boundsOptions.outWidth
    val sourceHeight = boundsOptions.outHeight
    if (sourceWidth <= 0 || sourceHeight <= 0) return null

    val decodeOptions =
        BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(sourceWidth, sourceHeight)
        }
    val decodedBitmap =
        openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } ?: return null
    val scaledBitmap = decodedBitmap.scaleDownTo(MAX_AVATAR_DIMENSION_PX)

    return ByteArrayOutputStream().use { outputStream ->
        var quality = JPEG_INITIAL_QUALITY
        var compressed: ByteArray? = null
        while (quality >= JPEG_MIN_QUALITY && compressed == null) {
            outputStream.reset()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val candidateBytes = outputStream.toByteArray()
            if (candidateBytes.size <= MAX_AVATAR_UPLOAD_BYTES) {
                compressed = candidateBytes
            }
            quality -= JPEG_QUALITY_STEP
        }
        compressed
    }
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
): Int {
    var sampleSize = 1
    while (width / sampleSize > MAX_AVATAR_DIMENSION_PX || height / sampleSize > MAX_AVATAR_DIMENSION_PX) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun Bitmap.scaleDownTo(maxDimension: Int): Bitmap {
    val maxSide = max(width, height)
    if (maxSide <= maxDimension) return this

    val scale = maxDimension.toFloat() / maxSide.toFloat()
    val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
    val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
}

private fun String.toJpegFileName(): String {
    val baseName = substringBeforeLast('.', missingDelimiterValue = this).ifBlank { DEFAULT_AVATAR_FILE_NAME }
    return "$baseName.jpg"
}

private fun Uri.displayName(contentResolver: ContentResolver): String? =
    contentResolver
        .query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex < 0) return@use null
            cursor.getString(nameIndex).takeIf { it.isNotBlank() }
        }

private const val DEFAULT_AVATAR_FILE_NAME = "avatar"
private const val DEFAULT_AVATAR_MIME_TYPE = "image/*"
private const val JPEG_MIME_TYPE = "image/jpeg"
private const val MAX_AVATAR_UPLOAD_BYTES = 4_718_592
private const val MAX_AVATAR_DIMENSION_PX = 1024
private const val JPEG_INITIAL_QUALITY = 90
private const val JPEG_MIN_QUALITY = 60
private const val JPEG_QUALITY_STEP = 10
