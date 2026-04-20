package com.example.lifetogether.data.repository

import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.repository.FamilyRepository
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FamilyRepositoryImpl @Inject constructor(
    private val userRepositoryImpl: UserRepositoryImpl,
) : FamilyRepository {

    override fun observeFamilyInformation(familyId: String): Flow<Result<FamilyInformation, String>> {
        return userRepositoryImpl.getFamilyInformation(familyId)
    }

    override suspend fun joinFamily(familyId: String, uid: String, name: String): Result<Unit, String> {
        return when (val result = userRepositoryImpl.joinFamily(familyId, uid, name)) {
            is ResultListener.Success -> Result.Success(Unit)
            is ResultListener.Failure -> Result.Failure(result.message)
        }
    }

    override suspend fun createNewFamily(uid: String, name: String): Result<Unit, String> {
        return when (val result = userRepositoryImpl.createNewFamily(uid, name)) {
            is ResultListener.Success -> Result.Success(Unit)
            is ResultListener.Failure -> Result.Failure(result.message)
        }
    }

    override suspend fun leaveFamily(familyId: String, uid: String): Result<Unit, String> {
        return when (val result = userRepositoryImpl.leaveFamily(familyId, uid)) {
            is ResultListener.Success -> Result.Success(Unit)
            is ResultListener.Failure -> Result.Failure(result.message)
        }
    }

    override suspend fun deleteFamily(familyId: String): Result<Unit, String> {
        return when (val result = userRepositoryImpl.deleteFamily(familyId)) {
            is ResultListener.Success -> Result.Success(Unit)
            is ResultListener.Failure -> Result.Failure(result.message)
        }
    }
}
