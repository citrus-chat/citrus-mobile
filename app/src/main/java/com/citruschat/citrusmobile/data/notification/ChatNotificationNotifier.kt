package com.citruschat.citrusmobile.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.citruschat.citrusmobile.MainActivity
import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.model.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatNotificationNotifier
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun showIncomingMessage(
            chatId: Long,
            chatName: String,
            message: Message,
        ) {
            ensureChannel()
            if (!canPostNotifications()) {
                return
            }

            val contentIntent =
                PendingIntent.getActivity(
                    context,
                    chatId.toInt(),
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(chatName.ifBlank { "New message" })
                    .setContentText(message.text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

            NotificationManagerCompat.from(context).notify(chatId.toInt(), notification)
        }

        private fun ensureChannel() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH,
                )
            manager.createNotificationChannel(channel)
        }

        private fun canPostNotifications(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    }

private const val CHANNEL_ID = "chat_messages"
private const val CHANNEL_NAME = "Chat messages"
