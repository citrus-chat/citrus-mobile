package com.citruschat.citrusmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.citruschat.citrusmobile.domain.model.Message
import com.citruschat.citrusmobile.ui.screen.ChatScreen
import com.citruschat.citrusmobile.ui.theme.CitrusMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CitrusMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CitrusChat()
                }
            }
        }
    }
}

@Preview
@Composable
fun CitrusChatPreview() {
    CitrusMobileTheme(darkTheme = false) {
        CitrusChat()
    }
}

@Composable
fun CitrusChat() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val sample = createSampleMessages()

        ChatScreen(
            initialMessages = sample,
            modifier = Modifier.padding(innerPadding),
            isInGroup = true,
        )
    }
}

private fun createSampleMessages(): List<Message> {
    val now = System.currentTimeMillis()
    val threeMinutes = 3 * 60 * 1000L

    return listOf(
        Message(
            id = 1,
            user = "Alice",
            text = "Hey, are you coming to the meeting later?",
            isOwn = false,
            timestamp = now - (4 * threeMinutes),
        ),
        Message(
            id = 2,
            user = "You",
            text = "Yeah — I'll be there in 10 minutes.",
            isOwn = true,
            timestamp = now - (3 * threeMinutes),
        ),
        Message(
            id = 3,
            user = "Alice",
            text = "Great! Don't forget to bring the reports.",
            isOwn = false,
            timestamp = now - (2 * threeMinutes),
        ),
        Message(
            id = 4,
            user = "You",
            text = "On it. See you soon.",
            isOwn = true,
            timestamp = now - threeMinutes,
        ),
        Message(
            id = 5,
            user = "Bob",
            text = "If anyone needs the agenda, I uploaded it to the drive.",
            isOwn = false,
            timestamp = now,
        ),
        Message(
            id = 6,
            user = "Crazy 8",
            text = "WHAT ARE YOU SAYING, BOB? I CAN'T HEAR YOU OVER THE SOUND OF MY OWN VOICE!",
            isOwn = false,
            timestamp = now,
        ),
    )
}
