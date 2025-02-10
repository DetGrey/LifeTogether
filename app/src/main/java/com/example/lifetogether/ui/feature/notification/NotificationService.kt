package com.example.lifetogether.ui.feature.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.lifetogether.MainActivity
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.sendFCMNotification
import com.example.lifetogether.util.Constants

class NotificationService(private val context: Context) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    // Method to create the notification channel with customizable parameters
    private fun createNotificationChannel(
        channelId: String,
        channelName: String,
        channelDescription: String,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
    ) {
        val existingChannel = notificationManager.getNotificationChannel(channelId)
        println("Existing channel: $existingChannel")
        if (existingChannel == null) {
            // Create the channel if it doesn't exist
            println("Creating notification channel: $channelName")
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun addNotificationChannels() {
        createNotificationChannel(
            Constants.GROCERY_LIST_CHANNEL,
            "Grocery List Notifications",
            "This is the channel for all Grocery List notifications",
        )
        createNotificationChannel(
            Constants.DEFAULT_CHANNEL,
            "Default Notifications",
            "This is the channel for all random notifications",
        )
        // ETC
    }

    // Method to show a customizable notification
    suspend fun sendNotification(
        tokens: List<String>,
        channelId: String,
        title: String,
        message: String,
        smallIconResId: Int = R.drawable.ic_logo,
        notificationId: Int,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        autoCancel: Boolean = true,
        destination: String? = null,
    ) {
        println("Trying to create and send notification: $message")

        sendFCMNotification(
            context,
            tokens,
            channelId, title, message, smallIconResId, notificationId, priority, autoCancel, destination,
        )
    }

    fun createNotification(
        channelId: String,
        title: String,
        message: String,
        smallIconResId: Int = R.drawable.ic_logo,
        notificationId: Int = System.currentTimeMillis().toInt(),
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        autoCancel: Boolean = true,
        destination: String? = null,
    ) {
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconResId)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setAutoCancel(autoCancel)

        destination?.let {
            notificationBuilder.setContentIntent(createPendingIntent(it))
        }

        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId, notification)
    }

    private fun createPendingIntent(destination: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("destination", destination)
        }

        return PendingIntent.getActivity(
            context,
            destination.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
