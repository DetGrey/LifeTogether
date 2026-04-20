package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.source.UserLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.listener.AuthResultListener
import com.example.lifetogether.domain.listener.ByteArrayResultListener
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
                    is AuthResultListener.Success -> {
                        runCatching {
                            // Check if user already has a profile image to avoid re-downloading
                            val hasExistingImage = result.userInformation.uid?.let { uidValue ->
                                userLocalDataSource.userHasProfileImage(uidValue)
                            } ?: false

                            if (!hasExistingImage) {
                                // Only download if image doesn't exist
                                val byteArrayResult: ByteArrayResultListener? =
                                    result.userInformation.imageUrl?.let { url ->
                                        storageDataSource.fetchImageByteArray(url)
                                    }

                                when (byteArrayResult) {
                                    is ByteArrayResultListener.Success -> {
                                        userLocalDataSource.updateUserInformation(
                                            result.userInformation,
                                            byteArrayResult.byteArray,
                                        )
                                    }

                                    is ByteArrayResultListener.Failure -> {
                                        println("ByteArrayResultListener failure: ${byteArrayResult.message}")
                                        // Update without image on failure
                                        userLocalDataSource.updateUserInformation(result.userInformation)
                                    }

                                    null -> {
                                        // No image URL provided, update without image
                                        userLocalDataSource.updateUserInformation(result.userInformation)
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
