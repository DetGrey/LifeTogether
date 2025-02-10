package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirebaseStorageDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.callback.FamilyInformationResultListener
import javax.inject.Inject

class ObserveFamilyInformationUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val firebaseStorageDataSource: FirebaseStorageDataSource,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke(
        familyId: String,
    ) {
        println("ObserveFamilyInformationUseCase invoked")
        firestoreDataSource.familyInformationSnapshotListener(familyId).collect { result ->
            println("familyInformationSnapshotListener().collect result: $result")
            when (result) {
                is FamilyInformationResultListener.Success -> {
                    val byteArrayResult: ByteArrayResultListener? = result.familyInformation.imageUrl?.let { url ->
                        firebaseStorageDataSource.downloadImage(url)
                    }

                    println("ObserveUserInformationUseCase biteArrayResult: $byteArrayResult")

                    when (byteArrayResult) {
                        is ByteArrayResultListener.Success -> {
                            localDataSource.updateFamilyInformation(result.familyInformation, byteArrayResult.byteArray)
                        }
                        is ByteArrayResultListener.Failure -> {
                            println("ByteArrayResultListener failure: ${byteArrayResult.message}")
                        }

                        null -> localDataSource.updateFamilyInformation(result.familyInformation)
                    }
                }
                is FamilyInformationResultListener.Failure -> {
                    // Handle failure
                    println("userInformationSnapshotListener failure: ${result.message}")
                }
            }
        }
    }
}
