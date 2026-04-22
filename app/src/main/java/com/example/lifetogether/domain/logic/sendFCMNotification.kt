package com.example.lifetogether.domain.logic

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.lifetogether.R
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpResponse
import com.google.auth.http.HttpCredentialsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private const val TAG = "SendFcmNotification"

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

    // Initialize the transport and request factory
    val transport = GoogleNetHttpTransport.newTrustedTransport()

    // Create the HTTP request factory with OAuth 2.0 credentials
    val requestFactory = transport.createRequestFactory(HttpCredentialsAdapter(credentials))

    // Build the URL for FCM HTTP v1 API endpoint
    val url = "https://fcm.googleapis.com/v1/projects/lifetogether-290b3/messages:send"

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

        // Create the HTTP request
        val request: HttpRequest = requestFactory.buildPostRequest(
            GenericUrl(url),
            ByteArrayContent.fromString("application/json", Json.encodeToString(requestBody)),
        )

        // Set the Authorization header with the OAuth token
        request.headers = HttpHeaders()
        request.headers.authorization = "Bearer ${credentials.accessToken}"

        // Send the request
        val response: HttpResponse = request.execute()

        // Check response
        if (response.statusCode == 200) {
            Log.d(TAG, "Notification sent successfully for token $i")
        } else {
            Log.e(TAG, "Failed to send notification for token $i: status=${response.statusCode}")
        }
    }
}
