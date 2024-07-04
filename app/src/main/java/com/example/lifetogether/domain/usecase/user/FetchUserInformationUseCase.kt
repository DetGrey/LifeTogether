package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.LocalUserRepositoryImpl
import com.example.lifetogether.domain.callback.AuthResultListener
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchUserInformationUseCase @Inject constructor(
    private val localUserRepositoryImpl: LocalUserRepositoryImpl,
) {
    operator fun invoke(uid: String): Flow<AuthResultListener> {
        println("FetchUserInformationUseCase invoked")
        return localUserRepositoryImpl.getUserInformation(uid)
    }
}
