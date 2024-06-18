package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val userRepository: RemoteUserRepositoryImpl,
) {
    suspend operator fun invoke(): ResultListener {
        return userRepository.logout()
    }
}
