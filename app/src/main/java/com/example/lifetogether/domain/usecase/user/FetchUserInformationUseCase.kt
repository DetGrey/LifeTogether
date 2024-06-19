package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.LocalUserRepositoryImpl
import javax.inject.Inject

class FetchUserInformationUseCase @Inject constructor(
    private val userRepository: LocalUserRepositoryImpl,
) {
    suspend operator fun invoke() {
        userRepository.getCurrentUser()
    }
}
