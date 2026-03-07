package com.example.lifetogether.domain.usecase.observers

import android.util.Log
import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.model.guides.Guide
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveGuidesUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
) {
    private companion object {
        const val TAG = "ObserveGuidesUseCase"
    }

    suspend operator fun invoke(
        uid: String,
        familyId: String,
    ) {
        Log.d(TAG, "invoke uid=$uid familyId=$familyId")

        combine(
            firestoreDataSource.familySharedGuidesSnapshotListener(familyId),
            firestoreDataSource.privateGuidesSnapshotListener(familyId, uid),
        ) { sharedResult, privateResult ->
            sharedResult to privateResult
        }.collect { (sharedResult, privateResult) ->
            val sharedGuides: List<Guide> = when (sharedResult) {
                is ListItemsResultListener.Success -> {
                    Log.d(TAG, "shared snapshot success count=${sharedResult.listItems.size}")
                    sharedResult.listItems
                }
                is ListItemsResultListener.Failure -> {
                    Log.e(TAG, "shared listener failure: ${sharedResult.message}")
                    return@collect
                }
            }

            val privateGuides: List<Guide> = when (privateResult) {
                is ListItemsResultListener.Success -> {
                    Log.d(TAG, "private snapshot success count=${privateResult.listItems.size}")
                    privateResult.listItems
                }
                is ListItemsResultListener.Failure -> {
                    Log.e(TAG, "private listener failure: ${privateResult.message}")
                    return@collect
                }
            }

            val mergedGuides = (sharedGuides + privateGuides)
                .associateBy { it.id ?: "" }
                .values
                .filter { !it.id.isNullOrBlank() }

            if (mergedGuides.isEmpty()) {
                Log.d(TAG, "merged guides empty -> deleteFamilyGuides familyId=$familyId")
                localDataSource.deleteFamilyGuides(familyId)
            } else {
                Log.d(TAG, "merged guides count=${mergedGuides.size} -> updateGuides familyId=$familyId")
                localDataSource.updateGuides(mergedGuides.toList())
            }
        }
    }
}
