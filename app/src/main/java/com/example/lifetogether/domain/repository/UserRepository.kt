package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun login(user: User): Result<UserInformation, String>
    suspend fun signUp(user: User, userInformation: UserInformation): Result<UserInformation, String>
    fun syncUserInformationFromRemote(uid: String): Flow<Result<Unit, String>>
    suspend fun changeName(uid: String, familyId: String?, newName: String): Result<Unit, String>
    suspend fun fetchFcmTokens(familyId: String): List<String>?
    suspend fun storeFcmToken(uid: String, familyId: String): Result<Unit, String>
}
