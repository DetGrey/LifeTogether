package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import com.example.lifetogether.domain.callback.AuthResultListener
import javax.inject.Inject

class FetchUserInformationUseCase @Inject constructor(
    private val userRepository: RemoteUserRepositoryImpl,
) {
    suspend operator fun invoke(): AuthResultListener {
        return userRepository.getCurrentUser()
    }
}
