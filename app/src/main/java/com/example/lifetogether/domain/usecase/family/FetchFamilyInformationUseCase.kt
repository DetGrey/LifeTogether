package com.example.lifetogether.domain.usecase.family

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchFamilyInformationUseCase @Inject constructor(
    private val userRepositoryImpl: UserRepositoryImpl,
) {
    operator fun invoke(familyId: String): Flow<Result<FamilyInformation, String>> {
        println("FetchFamilyInformationUseCase invoked")
        return userRepositoryImpl.getFamilyInformation(familyId)
    }
}
