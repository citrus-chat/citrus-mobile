package com.citruschat.citrusmobile.ui.screen

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.model.Message
import com.citruschat.citrusmobile.ui.components.ChatInput
import com.citruschat.citrusmobile.ui.components.MessageBubble

@Composable
fun ChatScreen(
    initialMessages: List<Message>,
    modifier: Modifier = Modifier,
    isInGroup: Boolean = false,
) {
    var messages = remember { mutableStateListOf<Message>().apply { addAll(initialMessages) } }
    var inputText by remember { mutableStateOf("") }

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
            itemsIndexed(items = messages, key = { _, msg -> msg.id }) { _, msg ->
                MessageBubble(message = msg, isInGroup = isInGroup)
            }
        }

        ChatInput(
            text = inputText,
            onValueChange = { newValue -> inputText = newValue },
            onClick = {
                if (inputText.isNotBlank()) {
                    val newMessage =
                        Message(
                            id = (messages.maxOfOrNull { it.id } ?: 0) + 1,
                            user = "You",
                            text = inputText,
                            isOwn = true,
                            timestamp = System.currentTimeMillis(),
                        )
                    messages.add(newMessage)
                    inputText = ""
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.padding_medium)),
        )
    }
}
