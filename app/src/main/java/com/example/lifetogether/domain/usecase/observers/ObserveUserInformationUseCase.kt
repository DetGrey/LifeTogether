package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.source.UserLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveUserInformationUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val storageDataSource: StorageDataSource,
    private val userLocalDataSource: UserLocalDataSource,
) {
    fun start(
        scope: CoroutineScope,
        uid: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            println("ObserveUserInformationUseCase invoked")
            firestoreDataSource.userInformationSnapshotListener(uid).collect { result ->
                println("userInformationSnapshotListener().collect result: $result")
                when (result) {
                    is AppResult.Success -> {
                        runCatching {
                            // Check if user already has a profile image to avoid re-downloading
                            val hasExistingImage = result.data.uid?.let { uidValue ->
                                userLocalDataSource.userHasProfileImage(uidValue)
                            } ?: false

                            if (!hasExistingImage) {
                                // Only download if image doesn't exist
                                val byteArrayResult: AppResult<ByteArray, String>? =
                                    result.data.imageUrl?.let { url ->
                                        storageDataSource.fetchImageByteArray(url)
                                    }

                                when (byteArrayResult) {
                                    is AppResult.Success -> {
                                        userLocalDataSource.updateUserInformation(
                                            result.data,
                                            byteArrayResult.data,
                                        )
                                    }

                                    is AppResult.Failure -> {
                                        println("ByteArrayResultListener failure: ${byteArrayResult.error}")
                                        // Update without image on failure
                                        userLocalDataSource.updateUserInformation(result.data)
                                    }

                                    null -> {
                                        // No image URL provided, update without image
                                        userLocalDataSource.updateUserInformation(result.data)
                                    }
                                }
                            } else {
                                println("ObserveUserInformationUseCase: Skipping download - user image already exists locally")
                                // Don't update to preserve existing image data
                            }
                        }
                            .onSuccess { firstSuccess.completeFirstSuccessIfNeeded() }
                            .onFailure { error ->
                                println("ObserveUserInformationUseCase local update failure: ${error.message}")
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
