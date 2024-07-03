package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.LocalUserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

class RemoveSavedUserInformationUseCase @Inject constructor(
    private val localLocalUserRepositoryImpl: LocalUserRepositoryImpl,
) {
    operator fun invoke(): ResultListener {
        println("RemoveSavedUserInformationUseCase invoked")
        return localLocalUserRepositoryImpl.removeSavedUserInformation()
    }
}
