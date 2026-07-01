package com.citruschat.citrusmobile.ui.chat.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.citruschat.citrusmobile.R

@Composable
fun ChatInput(
    text: String,
    onValueChange: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {},
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            modifier = Modifier.size(ATTACH_BUTTON_SIZE),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
            )
        }
        Spacer(Modifier.width(8.dp))
        TextField(
            value = text,
            onValueChange = onValueChange,
            placeholder = { Text(stringResource(R.string.chatInput_placeholder)) },
            shape = RoundedCornerShape(INPUT_CORNER_RADIUS),
            singleLine = false,
            maxLines = INPUT_MAX_LINES,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            modifier =
                Modifier
                    .weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onClick,
            enabled = text.isNotBlank(),
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ICON_ALPHA),
                ),
            modifier = Modifier.size(SEND_BUTTON_SIZE),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                modifier = Modifier.size(SEND_ICON_SIZE),
            )
        }
    }
}

private val ATTACH_BUTTON_SIZE = 44.dp
private val SEND_BUTTON_SIZE = 48.dp
private val SEND_ICON_SIZE = 20.dp
private val INPUT_CORNER_RADIUS = 24.dp
private const val INPUT_MAX_LINES = 4
private const val DISABLED_ICON_ALPHA = 0.62f
