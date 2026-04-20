package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.domain.repository.UserRepository
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveUserInformationUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    fun start(
        scope: CoroutineScope,
        uid: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<kotlin.Result<Unit>>()
        val job = scope.launch {
            userRepository.syncUserInformationFromRemote(uid).collect { result ->
                when (result) {
                    is AppResult.Success -> firstSuccess.completeFirstSuccessIfNeeded()
                    is AppResult.Failure -> println("user info sync failure: ${result.error}")
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
