package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun login(user: User): Result<UserInformation, AppError>
    suspend fun signUp(user: User, userInformation: UserInformation): Result<UserInformation, AppError>
    fun syncUserInformationFromRemote(uid: String): Flow<Result<Unit, AppError>>
    suspend fun changeName(uid: String, familyId: String?, newName: String): Result<Unit, AppError>
    suspend fun fetchFcmTokens(familyId: String): List<String>?
    suspend fun storeFcmToken(uid: String, familyId: String): Result<Unit, AppError>
}
