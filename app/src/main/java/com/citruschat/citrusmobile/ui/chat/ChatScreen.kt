package com.citruschat.citrusmobile.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.citruschat.citrusmobile.ui.chat.ChatViewModel
import com.citruschat.citrusmobile.ui.chat.component.ChatInput
import com.citruschat.citrusmobile.ui.chat.component.MessageBubble

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    isInGroup: Boolean = false,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.padding_small)),
        ) {
            itemsIndexed(uiState.messages, key = { _, msg -> msg.id }) { _, msg ->
                MessageBubble(message = msg, isInGroup = isInGroup)
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
