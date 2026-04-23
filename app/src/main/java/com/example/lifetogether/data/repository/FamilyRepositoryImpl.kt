package com.example.lifetogether.data.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.repository.FamilyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FamilyRepositoryImpl @Inject constructor(
    private val userRepositoryImpl: UserRepositoryImpl,
) : FamilyRepository {

    override fun observeFamilyInformation(familyId: String): Flow<Result<FamilyInformation, AppError>> {
        return userRepositoryImpl.observeFamilyInformation(familyId)
    }

    override fun syncFamilyInformationFromRemote(familyId: String): Flow<Result<Unit, AppError>> {
        return userRepositoryImpl.syncFamilyInformationFromRemote(familyId)
    }

    override suspend fun joinFamily(familyId: String, uid: String, name: String): Result<Unit, AppError> {
        return when (val result = userRepositoryImpl.joinFamily(familyId, uid, name)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    override suspend fun createNewFamily(uid: String, name: String): Result<Unit, AppError> {
        return when (val result = userRepositoryImpl.createNewFamily(uid, name)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    override suspend fun leaveFamily(familyId: String, uid: String): Result<Unit, AppError> {
        return when (val result = userRepositoryImpl.leaveFamily(familyId, uid)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    override suspend fun deleteFamily(familyId: String): Result<Unit, AppError> {
        return when (val result = userRepositoryImpl.deleteFamily(familyId)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }
}
