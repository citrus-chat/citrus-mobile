package com.citruschat.citrusmobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.model.Chat
import com.citruschat.citrusmobile.ui.home.component.ChatListItem

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onChatClick: (Chat) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "Chats",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))

        if (uiState.chats.isEmpty()) {
            Text(
                text = "No chats yet",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.padding_medium)),
            ) {
                itemsIndexed(
                    items = uiState.chats,
                    key = { _, chat -> chat.id },
                ) { _, chat ->
                    ChatListItem(
                        chat = chat,
                        onClick = { onChatClick(chat) },
                    )
                }
            }
        }
    }
}
