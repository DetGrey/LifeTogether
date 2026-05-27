package com.example.lifetogether.data.remote

import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.repository.UserRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.ui.feature.notification.NotificationService
import com.example.lifetogether.ui.navigation.NotificationDestination
import com.example.lifetogether.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var userRepository: UserRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
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
            val typedDestination = destination?.let(NotificationDestination::fromKey)

            notificationService.createNotification(
                channelId = channelId ?: Constants.DEFAULT_CHANNEL,
                title = title,
                message = message,
                smallIconResId = smallIconResId ?: R.drawable.ic_logo,
                notificationId = notificationId ?: System.currentTimeMillis().toInt(),
                priority = priority ?: NotificationCompat.PRIORITY_DEFAULT,
                autoCancel = autoCancel ?: true,
                destination = typedDestination,
            )
        }
    }
    // [END receive_message]

    // [START on_new_token]
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        val session = sessionRepository.sessionState.value as? SessionState.Authenticated
        val familyId = session?.user?.familyId
        if (session == null || familyId.isNullOrBlank()) {
            Log.d(TAG, "Skipping immediate FCM token registration because no authenticated family session is available")
            return
        }

        serviceScope.launch {
            when (val result = userRepository.storeFcmToken(session.user.uid, familyId, token)) {
                is Result.Success -> {
                    Log.d(TAG, "Stored refreshed FCM token for uid=${session.user.uid} familyId=$familyId")
                }

                is Result.Failure -> {
                    Log.w(TAG, "Failed to store refreshed FCM token for uid=${session.user.uid} familyId=$familyId error=${result.error}")
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
