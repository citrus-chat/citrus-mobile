package com.citruschat.citrusmobile.ui.profile.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.model.User

@Composable
fun ProfileHeader(
    user: User?,
    avatarLocalPath: String?,
    isAvatarUploading: Boolean,
    onAvatarClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            user = user,
            avatarLocalPath = avatarLocalPath,
            isUploading = isAvatarUploading,
            onClick = onAvatarClick,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.username?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_unknown_user),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = user?.email?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_unknown_email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    Spacer(modifier = Modifier.height(28.dp))

    ProfileField(label = stringResource(R.string.profile_status), value = user?.statusMessage?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_blank_status))
    ProfileField(label = stringResource(R.string.profile_created_at), value = user?.createdAt?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_blank_created_at))
}
