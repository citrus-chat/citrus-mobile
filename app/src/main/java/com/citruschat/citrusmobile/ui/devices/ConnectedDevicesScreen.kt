package com.citruschat.citrusmobile.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citruschat.citrusmobile.ui.devices.component.ConnectedDeviceRow
import com.citruschat.citrusmobile.ui.devices.component.ConnectedDevicesHeader
import com.citruschat.citrusmobile.ui.devices.component.EmptyConnectedDevicesState

@Composable
fun ConnectedDevicesScreen(
    onOpenQrScanner: () -> Unit,
    viewModel: ConnectedDevicesViewModel = hiltViewModel(),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        ConnectedDevicesHeader(onAddClick = onOpenQrScanner)

        Spacer(modifier = Modifier.height(24.dp))

        if (devices.isEmpty()) {
            EmptyConnectedDevicesState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = devices,
                    key = ConnectedDeviceUiModel::id,
                ) { device ->
                    ConnectedDeviceRow(device = device)
                }
            }
        }
    }
}
