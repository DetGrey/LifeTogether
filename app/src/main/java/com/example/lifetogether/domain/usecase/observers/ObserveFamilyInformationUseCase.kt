package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.repository.StorageRepository
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import com.example.lifetogether.domain.listener.FamilyInformationResultListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveFamilyInformationUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val storageRepository: StorageRepository,
    private val localDataSource: LocalDataSource,
) {
    fun start(
        scope: CoroutineScope,
        familyId: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            println("ObserveFamilyInformationUseCase invoked")
            firestoreDataSource.familyInformationSnapshotListener(familyId).collect { result ->
                println("familyInformationSnapshotListener().collect result: $result")
                when (result) {
                    is FamilyInformationResultListener.Success -> {
                        runCatching {
                            // Check if family already has an image to avoid re-downloading
                            val hasExistingImage = result.familyInformation.familyId?.let { familyIdValue ->
                                localDataSource.familyHasImage(familyIdValue)
                            } ?: false

                            if (!hasExistingImage) {
                                // Only download if image doesn't exist
                                val byteArrayResult: ByteArrayResultListener? =
                                    result.familyInformation.imageUrl?.let { url ->
                                        storageRepository.fetchImageByteArray(url)
                                    }

                                println("ObserveFamilyInformationUseCase byteArrayResult: $byteArrayResult")

                                when (byteArrayResult) {
                                    is ByteArrayResultListener.Success -> {
                                        localDataSource.updateFamilyInformation(
                                            result.familyInformation,
                                            byteArrayResult.byteArray,
                                        )
                                    }

                                    is ByteArrayResultListener.Failure -> {
                                        println("ByteArrayResultListener failure: ${byteArrayResult.message}")
                                        // Update without image on failure
                                        localDataSource.updateFamilyInformation(result.familyInformation)
                                    }

                                    null -> {
                                        // No image URL provided, update without image
                                        localDataSource.updateFamilyInformation(result.familyInformation)
                                    }
                                }
                            } else {
                                println("ObserveFamilyInformationUseCase: Skipping download - family image already exists locally")
                                // Don't update to preserve existing image data
                            }
                        }
                            .onSuccess { firstSuccess.completeFirstSuccessIfNeeded() }
                            .onFailure { error ->
                                println("ObserveFamilyInformationUseCase local update failure: ${error.message}")
                            }
                    }
                    is FamilyInformationResultListener.Failure -> {
                        // Keep listener alive; firstSuccess is one-shot and only completes on success.
                        println("userInformationSnapshotListener failure: ${result.message}")
                    }
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
