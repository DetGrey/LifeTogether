package com.example.lifetogether.domain.usecase.notification

import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import javax.inject.Inject

class FetchFcmTokensUseCase @Inject constructor(
    private val remoteUserRepositoryImpl: RemoteUserRepositoryImpl,
) {
    suspend operator fun invoke(
        familyId: String,
    ): List<String>? {
        return remoteUserRepositoryImpl.fetchFcmTokens(familyId)
    }
}
