package com.citruschat.citrusmobile.ui.profile.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.model.UserProfile

@Composable
fun ProfileOptions(
    profile: UserProfile?,
    isDarkTheme: Boolean,
    isLoggingOut: Boolean,
    isProfileSaving: Boolean,
    onShowPhoneChange: (Boolean) -> Unit,
    onShowEmailChange: (Boolean) -> Unit,
    onShowStatusChange: (Boolean) -> Unit,
    onShowDescriptionChange: (Boolean) -> Unit,
    onAllowGroupInvitesChange: (Boolean) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onConnectedDevicesClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val profileSwitchesEnabled = profile != null && !isProfileSaving

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        ProfileSwitchRow(
            label = stringResource(R.string.profile_show_phone),
            checked = profile?.showPhone ?: false,
            enabled = profileSwitchesEnabled,
            onCheckedChange = onShowPhoneChange,
        )
        ProfileSwitchRow(
            label = stringResource(R.string.profile_show_email),
            checked = profile?.showEmail ?: false,
            enabled = profileSwitchesEnabled,
            onCheckedChange = onShowEmailChange,
        )
        ProfileSwitchRow(
            label = stringResource(R.string.profile_show_status),
            checked = profile?.showStatus ?: false,
            enabled = profileSwitchesEnabled,
            onCheckedChange = onShowStatusChange,
        )
        ProfileSwitchRow(
            label = stringResource(R.string.profile_show_description),
            checked = profile?.showDescription ?: false,
            enabled = profileSwitchesEnabled,
            onCheckedChange = onShowDescriptionChange,
        )
        ProfileSwitchRow(
            label = stringResource(R.string.profile_allow_group_invites),
            checked = profile?.allowGroupInvites ?: false,
            enabled = profileSwitchesEnabled,
            onCheckedChange = onAllowGroupInvitesChange,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.profile_dark_mode),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Switch(
                checked = isDarkTheme,
                onCheckedChange = onDarkThemeChange,
            )
        }

        OutlinedButton(
            onClick = onConnectedDevicesClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.profile_other_devices))
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onLogoutClick,
            enabled = !isLoggingOut,
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text =
                    if (isLoggingOut) {
                        stringResource(R.string.profile_logging_out)
                    } else {
                        stringResource(R.string.profile_log_out)
                    },
            )
        }
    }
}

@Composable
private fun ProfileSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}
