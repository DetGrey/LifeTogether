package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface FamilyRepository {
    fun observeFamilyInformation(familyId: String): Flow<Result<FamilyInformation, AppError>>
    fun syncFamilyInformationFromRemote(familyId: String): Flow<Result<Unit, AppError>>
    suspend fun joinFamily(familyId: String, uid: String, name: String): Result<Unit, AppError>
    suspend fun createNewFamily(uid: String, name: String): Result<Unit, AppError>
    suspend fun leaveFamily(familyId: String, uid: String): Result<Unit, AppError>
    suspend fun deleteFamily(familyId: String): Result<Unit, AppError>
}
