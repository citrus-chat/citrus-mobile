package com.citruschat.citrusmobile.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.model.ChatParticipant
import com.citruschat.citrusmobile.ui.chat.component.ChatHeader
import com.citruschat.citrusmobile.ui.chat.component.ChatInput
import com.citruschat.citrusmobile.ui.chat.component.MessageBubble

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    onOpenChatProfile: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isInGroup = uiState.chat?.isGroup == true
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val keyboardHeight = WindowInsets.ime.getBottom(density)

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(keyboardHeight) {
        if (keyboardHeight > 0 && uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .background(MaterialTheme.colorScheme.background),
    ) {
        ChatHeader(
            chat = uiState.chat,
            onClick = onOpenChatProfile,
        )

        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imeNestedScroll(),
            contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.padding_small)),
        ) {
            itemsIndexed(uiState.messages, key = { _, msg -> msg.id }) { _, msg ->
                if (msg.id == uiState.chat?.firstUnreadMessageId) {
                    UnreadMessagesDivider()
                }
                MessageBubble(
                    message = msg,
                    isInGroup = isInGroup,
                    senderAvatarLocalPath = uiState.chat?.participants?.avatarFor(messageUser = msg.user),
                )
            }
        }

        ChatInput(
            text = uiState.inputText,
            onValueChange = viewModel::onInputChange,
            onClick = viewModel::sendMessage,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.padding_medium)),
        )
    }
}

@Composable
private fun UnreadMessagesDivider() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
        )
        Text(
            text = "Unread messages",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
        )
    }
}

private fun List<ChatParticipant>.avatarFor(messageUser: String): String? =
    firstOrNull { participant ->
        participant.username.equals(messageUser, ignoreCase = true)
    }?.localProfilePicturePath
