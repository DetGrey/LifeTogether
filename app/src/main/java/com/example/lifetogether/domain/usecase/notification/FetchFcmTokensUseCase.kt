package com.example.lifetogether.domain.usecase.notification

import com.example.lifetogether.data.repository.UserRepositoryImpl
import javax.inject.Inject

class FetchFcmTokensUseCase @Inject constructor(
    private val userRepositoryImpl: UserRepositoryImpl
) {
    suspend operator fun invoke(
        familyId: String,
    ): List<String>? {
        return userRepositoryImpl.fetchFcmTokens(familyId)
    }
}
