package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ListItemsResultListener
import javax.inject.Inject

class ObserveTipTrackerUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke(
        familyId: String,
    ) {
        println("ObserveTipTrackerUseCase invoked")
        firestoreDataSource.tipTrackerSnapshotListener(familyId).collect { result ->
            println("tipTrackerSnapshotListener().collect result: $result")
            when (result) {
                is ListItemsResultListener.Success -> {
                    if (result.listItems.isEmpty()) {
                        println("tipTrackerSnapshotListener().collect result: is empty")
                        localDataSource.deleteFamilyTipItems(familyId)
                    } else {
                        localDataSource.updateTipTracker(result.listItems)
                    }
                }
                is ListItemsResultListener.Failure -> {
                    // Handle failure
                    println("ObserveFirestoreUseCase failure: ${result.message}")
                }
            }
        }
    }
}
