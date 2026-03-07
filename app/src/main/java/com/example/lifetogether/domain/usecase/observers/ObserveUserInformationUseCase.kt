package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.repository.StorageRepository
import com.example.lifetogether.domain.listener.AuthResultListener
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveUserInformationUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val storageRepository: StorageRepository,
    private val localDataSource: LocalDataSource,
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
                    is AuthResultListener.Success -> {
                        runCatching {
                            // Check if user already has a profile image to avoid re-downloading
                            val hasExistingImage = result.userInformation.uid?.let { uidValue ->
                                localDataSource.userHasProfileImage(uidValue)
                            } ?: false

                            if (!hasExistingImage) {
                                // Only download if image doesn't exist
                                val byteArrayResult: ByteArrayResultListener? =
                                    result.userInformation.imageUrl?.let { url ->
                                        storageRepository.fetchImageByteArray(url)
                                    }

                                when (byteArrayResult) {
                                    is ByteArrayResultListener.Success -> {
                                        localDataSource.updateUserInformation(
                                            result.userInformation,
                                            byteArrayResult.byteArray,
                                        )
                                    }

                                    is ByteArrayResultListener.Failure -> {
                                        println("ByteArrayResultListener failure: ${byteArrayResult.message}")
                                        // Update without image on failure
                                        localDataSource.updateUserInformation(result.userInformation)
                                    }

                                    null -> {
                                        // No image URL provided, update without image
                                        localDataSource.updateUserInformation(result.userInformation)
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
                    is AuthResultListener.Failure -> {
                        // Keep listener alive; firstSuccess is one-shot and only completes on success.
                        println("userInformationSnapshotListener failure: ${result.message}")
                    }
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
