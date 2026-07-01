package com.citruschat.citrusmobile.ui.home.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.model.ChatListItemSummary
import com.citruschat.citrusmobile.domain.model.MessageDeliveryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ChatListItemComponent(
    chat: ChatListItemSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowColor by animateColorAsState(
        targetValue =
            when {
                isPressed -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
            },
        label = "chatRowColor",
    )

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        color = rowColor,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) { onClick() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatAvatar(
                name = chat.name,
                avatarLocalPath = chat.localProfilePicturePath,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = chat.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LastMessageStatusIcon(status = chat.lastMessageStatus)

                    val previewText = chat.activityText?.takeIf { it.isNotBlank() } ?: chat.lastMessagePreview.orEmpty()
                    Text(
                        text = previewText.ifBlank { stringResource(R.string.chat_item_empty_chat) },
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (chat.activityText.isNullOrBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (chat.unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                UnreadCountBubble(count = chat.unreadCount)
            }
        }
    }
}

@Composable
private fun UnreadCountBubble(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = count.badgeText(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun ChatAvatar(
    name: String,
    avatarLocalPath: String?,
    size: Dp = 52.dp,
) {
    val initial =
        name
            .trim()
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "?"

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        avatarLocalPath,
    ) {
        value = avatarLocalPath?.let { loadBitmap(it) }
    }

    Surface(
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap == null) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun LastMessageStatusIcon(status: MessageDeliveryStatus?) {
    val icon = status?.icon() ?: return
    val tint = status.tint()

    Icon(
        imageVector = icon,
        contentDescription = status.label(),
        tint = tint,
        modifier =
            Modifier
                .padding(end = 4.dp)
                .size(16.dp),
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

private fun Int.badgeText(): String = if (this > MAX_BADGE_COUNT) MAX_BADGE_TEXT else toString()

private suspend fun loadBitmap(path: String) =
    withContext(Dispatchers.IO) {
        BitmapFactory.decodeFile(path)
    }

private const val MAX_BADGE_COUNT = 99
private const val MAX_BADGE_TEXT = "99+"
