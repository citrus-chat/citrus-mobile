package com.citruschat.citrusmobile.ui.chat.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.model.Message
import com.citruschat.citrusmobile.domain.model.MessageDeliveryStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    message: Message,
    isInGroup: Boolean = false,
    senderAvatarLocalPath: String? = null,
    bubbleMaxWidth: Dp = 280.dp,
) {
    val bubbleColor: Color =
        if (message.isOwn)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant
    val contentColor: Color =
        if (message.isOwn)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier =
            modifier.fillMaxWidth().padding(
                vertical = dimensionResource(R.dimen.padding_small),
                horizontal = dimensionResource(R.dimen.padding_medium),
            ),
        horizontalArrangement = if (message.isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (isInGroup && !message.isOwn) {
            ChatAvatar(
                name = message.user,
                avatarLocalPath = senderAvatarLocalPath,
                size = 32.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape =
                RoundedCornerShape(
                    dimensionResource(R.dimen.corner_radius_small),
                    dimensionResource(R.dimen.corner_radius_large),
                    dimensionResource(R.dimen.corner_radius_small),
                    dimensionResource(R.dimen.corner_radius_large),
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor = bubbleColor,
                    contentColor = contentColor,
                ),
            modifier =
                Modifier
                    .widthIn(max = bubbleMaxWidth)
                    .wrapContentWidth(),
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
            ) {
                if (isInGroup && !message.isOwn) {
                    Text(
                        text = message.user,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                )

                val timeText =
                    Instant
                        .ofEpochMilli(message.timestamp)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm"))

                Row(
                    modifier =
                        Modifier
                            .align(Alignment.End)
                            .padding(top = dimensionResource(R.dimen.padding_xsmall)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    MessageStatus(status = message.deliveryStatus)
                }
            }
        }
    }
}

@Composable
private fun MessageStatus(status: MessageDeliveryStatus) {
    Icon(
        imageVector = status.icon(),
        contentDescription = status.label(),
        tint = status.tint(),
        modifier = Modifier.size(15.dp),
    )
}

@Composable
private fun MessageDeliveryStatus.tint(): Color =
    when (this) {
        MessageDeliveryStatus.VIEWED -> MaterialTheme.colorScheme.tertiary
        MessageDeliveryStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

private fun MessageDeliveryStatus.icon(): ImageVector =
    when (this) {
        MessageDeliveryStatus.SENDING -> Icons.Default.Schedule
        MessageDeliveryStatus.SENT -> Icons.Default.Done
        MessageDeliveryStatus.DELIVERED -> Icons.Default.DoneAll
        MessageDeliveryStatus.VIEWED -> Icons.Default.DoneAll
        MessageDeliveryStatus.FAILED -> Icons.Default.ErrorOutline
    }

private fun MessageDeliveryStatus.label(): String =
    when (this) {
        MessageDeliveryStatus.SENDING -> "Sending"
        MessageDeliveryStatus.SENT -> "Sent"
        MessageDeliveryStatus.DELIVERED -> "Delivered"
        MessageDeliveryStatus.VIEWED -> "Viewed"
        MessageDeliveryStatus.FAILED -> "Failed"
    }
