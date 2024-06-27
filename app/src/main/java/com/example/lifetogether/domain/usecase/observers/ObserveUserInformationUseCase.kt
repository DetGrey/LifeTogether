package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.UserListListener
import javax.inject.Inject

class ObserveUserInformationUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke() {
        println("ObserveUserInformationUseCase invoked")
        firestoreDataSource.userInformationSnapshotListener().collect { result ->
            println("categoriesSnapshotListener().collect result: $result")
            when (result) {
                is UserListListener.Success -> {
                    localDataSource.updateUserInformation(result.userInformationList)
                }
                is UserListListener.Failure -> {
                    // Handle failure
                    println("categoriesSnapshotListener failure: ${result.message}")
                }
            }
        }
    }
}
