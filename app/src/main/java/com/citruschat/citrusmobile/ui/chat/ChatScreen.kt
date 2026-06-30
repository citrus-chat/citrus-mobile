package com.citruschat.citrusmobile.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.model.ChatParticipant
import com.citruschat.citrusmobile.ui.chat.component.ChatInput
import com.citruschat.citrusmobile.ui.chat.component.ChatHeader
import com.citruschat.citrusmobile.ui.chat.component.MessageBubble

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    onOpenChatProfile: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isInGroup = uiState.chat?.isGroup == true

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
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.padding_small)),
        ) {
            itemsIndexed(uiState.messages, key = { _, msg -> msg.id }) { _, msg ->
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

private fun List<ChatParticipant>.avatarFor(messageUser: String): String? =
    firstOrNull { participant ->
        participant.username.equals(messageUser, ignoreCase = true)
    }?.localProfilePicturePath
