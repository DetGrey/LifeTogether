package com.example.lifetogether.domain.logic

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

private const val TAG = "ServiceAccountAuth"

suspend fun getServiceAccountAccessToken(context: Context): GoogleCredentials? = withContext(Dispatchers.IO) {
    try {
        // Access the file from the assets directory
        val inputStream: InputStream = context.assets.open("lifetogether-290b3-337209fd5301.json")
        val credentials = ServiceAccountCredentials
            .fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        Log.d(TAG, "Service account credentials loaded")

        // Refresh the access token on a background thread
        // credentials.refreshAccessToken() // Removed this line

        return@withContext credentials
    } catch (e: Exception) {
        Log.e(TAG, "Error loading service account credentials: ${e.message}", e)
        return@withContext null
    }
}
