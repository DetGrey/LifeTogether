package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.AuthResultListener
import javax.inject.Inject

class ObserveUserInformationUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
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
                    localDataSource.updateUserInformation(result.userInformation)
                }
                is AuthResultListener.Failure -> {
                    // Handle failure
                    println("userInformationSnapshotListener failure: ${result.message}")
                }
            }
        }
    }
}
