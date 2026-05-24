package com.example.lifetogether.domain.usecase.notification

import com.example.lifetogether.domain.repository.UserRepository
import com.example.lifetogether.ui.feature.notification.NotificationService
import com.example.lifetogether.ui.navigation.NotificationDestination
import javax.inject.Inject

class SendNotificationUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
) {
    suspend operator fun invoke(
        familyId: String,
        title: String,
        message: String,
        channelId: String,
        destination: NotificationDestination,
    ) {
        val tokens = userRepository.fetchFcmTokens(familyId) ?: return
        if (tokens.isEmpty()) return

        notificationService.sendNotification(
            tokens = tokens,
            channelId = channelId,
            title = title,
            message = message,
            notificationId = System.currentTimeMillis().toInt(),
            destination = destination.key,
        )
    }
}