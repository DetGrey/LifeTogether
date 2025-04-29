package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.ListItemsResultListener
import javax.inject.Inject

class ObserveAlbumsUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke(
        familyId: String,
    ) {
        println("ObserveAlbumsUseCase invoked")
        firestoreDataSource.albumsSnapshotListener(familyId).collect { result ->
            println("albumsSnapshotListener().collect result: $result")
            when (result) {
                is ListItemsResultListener.Success -> {
                    if (result.listItems.isEmpty()) {
                        println("albumsSnapshotListener().collect result: is empty")
                        localDataSource.deleteFamilyAlbums(familyId)
                    } else {
                        localDataSource.updateAlbums(result.listItems)
                    }
                }
                is ListItemsResultListener.Failure -> {
                    // Handle failure
                    println("ObserveAlbumsUseCase failure: ${result.message}")
                }
            }
        }
    }
}
