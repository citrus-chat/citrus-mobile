package com.citruschat.citrusmobile.ui.chat.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.model.Message
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    message: Message,
    isInGroup: Boolean = false,
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

    // Una row para alinear el mensaje a la derecha o izquierda dependiendo de si es propio o no
    Row(
        modifier =
            modifier.fillMaxWidth().padding(
                vertical = dimensionResource(R.dimen.padding_small),
                horizontal = dimensionResource(R.dimen.padding_medium),
            ),
        horizontalArrangement = if (message.isOwn) Arrangement.End else Arrangement.Start,
    ) {
        // Un card para el mensaje con bordes redondeados y un color de fondo diferente para mensajes propios y ajenos
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
                // Si el mensaje es parte de un grupo y no es propio,
                // mostramos el nombre del usuario encima del mensaje
                if (isInGroup && !message.isOwn) {
                    Text(
                        text = message.user,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
                }

                // El texto del mensaje
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                )

                // La hora del mensaje en formato simple debajo del texto
                val timeText =
                    Instant
                        .ofEpochMilli(message.timestamp)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    modifier =
                        Modifier
                            .align(Alignment.End)
                            .padding(top = dimensionResource(R.dimen.padding_xsmall)),
                )
            }
        }
    }
}
