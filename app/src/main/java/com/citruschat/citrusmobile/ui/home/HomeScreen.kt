package com.citruschat.citrusmobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.ui.home.component.ChatListItemComponent
import com.citruschat.citrusmobile.ui.home.component.UserSearchResultComponent

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenChat: (Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.openChatEvents.collect { chatId ->
            onOpenChat(chatId)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "Chats",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.chats_search_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.chats_search_clear),
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
        }

        if (uiState.chats.isEmpty() && uiState.userResults.isEmpty()) {
            Text(
                text = stringResource(R.string.chats_search_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val chatsTitle = stringResource(R.string.chats_search_chats_title)
            val usersTitle = stringResource(R.string.chats_search_users_title)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.padding_medium)),
            ) {
                if (uiState.chats.isNotEmpty()) {
                    itemHeader(text = chatsTitle)
                    items(
                        items = uiState.chats,
                        key = { chat -> "chat-${chat.id}" },
                    ) { chat ->
                        ChatListItemComponent(
                            chat = chat,
                            onClick = { onOpenChat(chat.id) },
                        )
                    }
                }

                if (uiState.userResults.isNotEmpty()) {
                    itemHeader(text = usersTitle)
                    items(
                        items = uiState.userResults,
                        key = { user -> "user-${user.id}" },
                    ) { user ->
                        UserSearchResultComponent(
                            user = user,
                            onClick = { viewModel.onUserResultClick(user) },
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemHeader(text: String) {
    item(key = "header-$text") {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        )
    }
}
