package com.example.lifetogether.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.usecase.notification.FetchFcmTokensUseCase
import com.example.lifetogether.ui.feature.notification.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val fetchFcmTokensUseCase: FetchFcmTokensUseCase,
) : ViewModel() {
    fun sendNotification(
        context: Context,
        familyId: String,
        title: String,
        message: String,
        channelId: String,
        destination: String,
    ) {
        val notificationService = NotificationService(context)

        viewModelScope.launch {
            val tokens: List<String>? = fetchFcmTokensUseCase(familyId)

            println("Tokens: $tokens")

//            tokens = listOf("c90zI9KTQwS0jR5VJKPNej:APA91bHhuJ5pWfr9IfWSjTXDRkORuU-JyVGAp6Ds9yP1jfOqyRDahtdZem2WyQzkiB0ft1L3p6Sf4zkyK7Inotxg9Tw9Hnn0Es6hjZyr3fR0-5DHXgKZk44",)

            if (tokens != null) {
                notificationService.sendNotification(
                    tokens = tokens,
                    channelId = channelId,
                    title = title,
                    message = message,
                    notificationId = System.currentTimeMillis().toInt(),
                    destination = destination,
                )
            }
        }
    }
}
