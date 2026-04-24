package com.example.lifetogether.domain.logic

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

suspend fun getServiceAccountAccessToken(context: Context): GoogleCredentials? = withContext(Dispatchers.IO) {
    try {
        // Access the file from the assets directory
        val inputStream: InputStream = context.assets.open("lifetogether-290b3-337209fd5301.json")
        val credentials = ServiceAccountCredentials
            .fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

        println("Credentials: $credentials")

        // Refresh the access token on a background thread
        // credentials.refreshAccessToken() // Removed this line

        return@withContext credentials
    } catch (e: Exception) {
        println("Error loading service account credentials: ${e.message}")
        e.printStackTrace()
        println("Exception type: ${e::class.java.name}") // Print the type of exception
        return@withContext null
    }
}
