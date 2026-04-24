package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.UserInformation
import kotlinx.coroutines.flow.Flow

interface SessionUserRepository {
    fun removeSavedUserInformation(): Result<Unit, String>
    suspend fun logout(uid: String, familyId: String?): Result<Unit, String>
    suspend fun fetchUserInformation(uid: String): Result<UserInformation, String>
    fun observeUserInformation(uid: String): Flow<Result<UserInformation, String>>
}
