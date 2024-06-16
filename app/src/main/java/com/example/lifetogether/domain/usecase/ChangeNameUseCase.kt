package com.example.lifetogether.domain.usecase

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener

class ChangeNameUseCase {
    private val userRepository = UserRepositoryImpl()
    suspend operator fun invoke(
        uid: String,
        newName: String,
    ): ResultListener {
        return userRepository.changeName(uid, newName)
    }
}
