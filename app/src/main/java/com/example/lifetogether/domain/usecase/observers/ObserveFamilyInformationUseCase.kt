package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.domain.repository.FamilyRepository
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveFamilyInformationUseCase @Inject constructor(
    private val familyRepository: FamilyRepository,
) {
    fun start(
        scope: CoroutineScope,
        familyId: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<kotlin.Result<Unit>>()
        val job = scope.launch {
            familyRepository.syncFamilyInformationFromRemote(familyId).collect { result ->
                when (result) {
                    is AppResult.Success -> firstSuccess.completeFirstSuccessIfNeeded()
                    is AppResult.Failure -> println("family info sync failure: ${result.error}")
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
