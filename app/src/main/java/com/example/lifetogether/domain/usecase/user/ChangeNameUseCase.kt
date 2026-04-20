package com.example.lifetogether.domain.usecase.user

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import javax.inject.Inject

class ChangeNameUseCase @Inject constructor(
    private val userRepositoryImpl: UserRepositoryImpl
) {
    suspend operator fun invoke(
        uid: String,
        familyId: String?,
        newName: String,
    ): ResultListener {
        return userRepositoryImpl.changeName(uid, familyId, newName)
    }
}
