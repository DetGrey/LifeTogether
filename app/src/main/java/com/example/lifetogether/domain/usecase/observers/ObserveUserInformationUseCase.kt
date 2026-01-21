package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.repository.StorageRepository
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import javax.inject.Inject

class ObserveUserInformationUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val storageRepository: StorageRepository,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke(
        uid: String,
    ) {
        println("ObserveUserInformationUseCase invoked")
        firestoreDataSource.userInformationSnapshotListener(uid).collect { result ->
            println("userInformationSnapshotListener().collect result: $result")
            when (result) {
                is AuthResultListener.Success -> {
                    // Check if user already has a profile image to avoid re-downloading
                    val hasExistingImage = result.userInformation.uid?.let { uid ->
                        localDataSource.userHasProfileImage(uid)
                    } ?: false

                    if (!hasExistingImage) {
                        // Only download if image doesn't exist
                        val byteArrayResult: ByteArrayResultListener? =
                            result.userInformation.imageUrl?.let { url ->
                                storageRepository.fetchImageByteArray(url)
                            }

//                    println("ObserveUserInformationUseCase byteArrayResult: $byteArrayResult")

                        when (byteArrayResult) {
                            is ByteArrayResultListener.Success -> {
                                localDataSource.updateUserInformation(result.userInformation, byteArrayResult.byteArray)
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
                is AuthResultListener.Failure -> {
                    // Handle failure
                    println("userInformationSnapshotListener failure: ${result.message}")
                }
            }
        }
    }
}
