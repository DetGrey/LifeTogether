package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

class ChangeNameUseCase @Inject constructor(
    private val userRepository: RemoteUserRepositoryImpl,
) {
    suspend operator fun invoke(
        uid: String,
        newName: String,
    ): ResultListener {
        return userRepository.changeName(uid, newName)
    }
}
