package com.example.lifetogether.domain.usecase.family

import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.listener.FamilyInformationResultListener
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchFamilyInformationUseCase @Inject constructor(
    private val userRepositoryImpl: UserRepositoryImpl,
) {
    operator fun invoke(familyId: String): Flow<FamilyInformationResultListener> {
        println("FetchFamilyInformationUseCase invoked")
        return userRepositoryImpl.getFamilyInformation(familyId)
    }
}
