package com.citruschat.citrusmobile.ui.profile.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.model.UserProfile

@Composable
fun ProfileHeader(
    profile: UserProfile?,
    avatarLocalPath: String?,
    isAvatarUploading: Boolean,
    description: String,
    isProfileSaving: Boolean,
    onDescriptionChange: (String) -> Unit,
    onDescriptionSave: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            username = profile?.username,
            avatarLocalPath = avatarLocalPath,
            isUploading = isAvatarUploading,
            onClick = onAvatarClick,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile?.username?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_unknown_user),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.profile_privacy_value,
                    stringResource(profile?.privacy.privacyLabelRes()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    ProfileField(
        label = stringResource(R.string.profile_email),
        value = profile?.email?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_unknown_email),
    )


    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        enabled = profile != null && !isProfileSaving,
        label = { Text(text = stringResource(R.string.profile_about_me)) },
        trailingIcon = {
            IconButton(
                onClick = onDescriptionSave,
                enabled = profile != null && !isProfileSaving && description != profile.description,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.profile_save_description),
                )
            }
        },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth(),
    )
}

@StringRes
private fun String?.privacyLabelRes(): Int =
    when (this) {
        "public" -> R.string.profile_privacy_public
        "contacts" -> R.string.profile_privacy_contacts
        "private" -> R.string.profile_privacy_private
        else -> R.string.profile_privacy_unknown
    }
