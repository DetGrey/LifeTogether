package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun login(user: User): Result<Unit, AppError>
    suspend fun signUp(user: User, userInformation: UserInformation): Result<UserInformation, AppError>
    fun syncUserInformationFromRemote(uid: String): Flow<Result<Unit, AppError>>
    suspend fun changeName(uid: String, familyId: String?, newName: String): Result<Unit, AppError>
    suspend fun fetchFcmTokens(familyId: String): List<String>?
    suspend fun storeFcmToken(uid: String, familyId: String, token: String? = null): Result<Unit, AppError>
    fun observeAdminUids(): Flow<Result<List<String>, AppError>>
    suspend fun addAdminUid(requesterUid: String, targetUid: String): Result<Unit, AppError>
    suspend fun removeAdminUid(requesterUid: String, targetUid: String): Result<Unit, AppError>
}
