package com.example.lifetogether.domain.usecase.family

import com.example.lifetogether.data.repository.LocalUserRepositoryImpl
import com.example.lifetogether.domain.callback.FamilyInformationResultListener
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchFamilyInformationUseCase @Inject constructor(
    private val localUserRepositoryImpl: LocalUserRepositoryImpl,
) {
    operator fun invoke(familyId: String): Flow<FamilyInformationResultListener> {
        println("FetchFamilyInformationUseCase invoked")
        return localUserRepositoryImpl.getFamilyInformation(familyId)
    }
}
