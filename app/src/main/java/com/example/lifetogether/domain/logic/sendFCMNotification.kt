package com.example.lifetogether.domain.logic

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.lifetogether.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val TAG = "SendFcmNotification"
private const val FCM_SEND_URL = "https://fcm.googleapis.com/v1/projects/lifetogether-290b3/messages:send"

private val fcmHttpClient by lazy { OkHttpClient() }

suspend fun sendFCMNotification(
    context: Context,
    tokens: List<String>,
    channelId: String,
    title: String,
    message: String,
    smallIconResId: Int = R.drawable.ic_logo,
    notificationId: Int,
    priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    autoCancel: Boolean = true,
    destination: String? = null,
) = withContext(Dispatchers.IO) {
    Log.d(TAG, "Preparing to send notifications. tokenCount=${tokens.size}")
    val credentials = getServiceAccountAccessToken(context)

    if (credentials == null) {
        Log.e(TAG, "Failed to get service account credentials")
        return@withContext
    }

    credentials.refreshIfExpired()
    val accessToken = credentials.accessToken?.tokenValue
    if (accessToken.isNullOrBlank()) {
        Log.e(TAG, "Missing access token for FCM request")
        return@withContext
    }

    val data = mapOf(
        "channelId" to channelId,
        "smallIconResId" to smallIconResId.toString(),
        "notificationId" to notificationId.toString(),
        "priority" to priority.toString(),
        "autoCancel" to autoCancel.toString(),
        "destination" to (destination ?: ""),
    )
    var i = 0
    for (token in tokens) {
        i += 1
        Log.d(TAG, "Sending notification to token $i of ${tokens.size}")

        val requestBody = buildJsonObject {
            putJsonObject("message") {
                put("token", token)
                putJsonObject("notification") {
                    put("title", title)
                    put("body", message)
                }
                putJsonObject("data") {
                    data.forEach { (key, value) ->
                        put(key, value)
                    }
                }
            }
        }

        val request = Request.Builder()
            .url(FCM_SEND_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(
                Json.encodeToString(requestBody)
                    .toRequestBody("application/json".toMediaType()),
            )
            .build()

        fcmHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                Log.d(TAG, "Notification sent successfully for token $i")
            } else {
                val responseBody = response.body?.string().orEmpty()
                Log.e(
                    TAG,
                    "Failed to send notification for token $i: status=${response.code} body=$responseBody",
                )
            }
        }
    }
}
