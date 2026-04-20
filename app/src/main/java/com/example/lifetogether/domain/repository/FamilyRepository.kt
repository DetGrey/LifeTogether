package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface FamilyRepository {
    fun observeFamilyInformation(familyId: String): Flow<Result<FamilyInformation, String>>
    fun syncFamilyInformationFromRemote(familyId: String): Flow<Result<Unit, String>>
    suspend fun joinFamily(familyId: String, uid: String, name: String): Result<Unit, String>
    suspend fun createNewFamily(uid: String, name: String): Result<Unit, String>
    suspend fun leaveFamily(familyId: String, uid: String): Result<Unit, String>
    suspend fun deleteFamily(familyId: String): Result<Unit, String>
}
