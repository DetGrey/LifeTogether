package com.example.lifetogether.data.remote

import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.lifetogether.R
import com.example.lifetogether.ui.feature.notification.NotificationService
import com.example.lifetogether.util.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            val title = remoteMessage.notification?.title ?: "Default Title"
            val message = remoteMessage.notification?.body ?: "Default Message"
            val channelId = remoteMessage.data["channelId"]
            val smallIconResId = remoteMessage.data["smallIconResId"]?.toIntOrNull()
            val notificationId = remoteMessage.data["notificationId"]?.toIntOrNull()
            val priority = remoteMessage.data["priority"]?.toIntOrNull()
            val autoCancel = remoteMessage.data["autoCancel"]?.toBoolean()
            val destination = remoteMessage.data["destination"]

            // TODO here goes the logic after receiving data
            // Create an instance of NotificationService and send the notification
            val notificationService = NotificationService(applicationContext)
            notificationService.createNotification(
                channelId = channelId ?: Constants.DEFAULT_CHANNEL,
                title = title,
                message = message,
                smallIconResId = smallIconResId ?: R.drawable.ic_logo,
                notificationId = notificationId ?: System.currentTimeMillis().toInt(),
                priority = priority ?: NotificationCompat.PRIORITY_DEFAULT,
                autoCancel = autoCancel ?: true,
                destination = destination,
            )
        }
    }
    // [END receive_message]

    // [START on_new_token]
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
