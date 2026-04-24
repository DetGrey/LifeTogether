package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.source.UserLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveFamilyInformationUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val storageDataSource: StorageDataSource,
    private val userLocalDataSource: UserLocalDataSource,
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
                    is AppResult.Success -> {
                        runCatching {
                            // Check if family already has an image to avoid re-downloading
                            val hasExistingImage = result.data.familyId?.let { familyIdValue ->
                                userLocalDataSource.familyHasImage(familyIdValue)
                            } ?: false

                            if (!hasExistingImage) {
                                // Only download if image doesn't exist
                                val byteArrayResult: AppResult<ByteArray, String>? =
                                    result.data.imageUrl?.let { url ->
                                        storageDataSource.fetchImageByteArray(url)
                                    }

                                println("ObserveFamilyInformationUseCase byteArrayResult: $byteArrayResult")

                                when (byteArrayResult) {
                                    is AppResult.Success -> {
                                        userLocalDataSource.updateFamilyInformation(
                                            result.data,
                                            byteArrayResult.data,
                                        )
                                    }

                                    is AppResult.Failure -> {
                                        println("ByteArrayResultListener failure: ${byteArrayResult.error}")
                                        // Update without image on failure
                                        userLocalDataSource.updateFamilyInformation(result.data)
                                    }

                                    null -> {
                                        // No image URL provided, update without image
                                        userLocalDataSource.updateFamilyInformation(result.data)
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
                    is AppResult.Failure -> {
                        // Keep listener alive; firstSuccess is one-shot and only completes on success.
                        println("userInformationSnapshotListener failure: ${result.error}")
                    }
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
