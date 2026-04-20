package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.repository.SessionRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): ResultListener = sessionRepository.signOut()
}
