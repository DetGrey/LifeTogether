package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.source.GroceryLocalDataSource
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ListItemsResultListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveGroceryListUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val groceryLocalDataSource: GroceryLocalDataSource,
) {
    fun start(
        scope: CoroutineScope,
        familyId: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            println("ObserveGroceryListUseCase invoked")
            firestoreDataSource.grocerySnapshotListener(familyId).collect { result ->
                println("grocerySnapshotListener().collect result: $result")
                when (result) {
                    is ListItemsResultListener.Success -> {
                        runCatching {
                            if (result.listItems.isEmpty()) {
                                println("grocerySnapshotListener().collect result: is empty")
                                groceryLocalDataSource.deleteFamilyGroceryItems(familyId)
                            } else {
                                val entities = result.listItems.map { item ->
                                    GroceryListEntity(
                                        id = item.id ?: "",
                                        familyId = item.familyId,
                                        name = item.itemName,
                                        lastUpdated = item.lastUpdated,
                                        completed = item.completed,
                                        category = item.category,
                                        approxPrice = item.approxPrice,
                                    )
                                }
                                groceryLocalDataSource.updateGroceryList(
                                    familyId,
                                    entities
                                )
                            }
                        }.onSuccess {
                            firstSuccess.completeFirstSuccessIfNeeded()
                        }.onFailure { error ->
                            println("ObserveGroceryListUseCase local update failure: ${error.message}")
                        }
                    }
                    is ListItemsResultListener.Failure -> {
                        // Keep listener alive; firstSuccess is one-shot and only completes on success.
                        println("ObserveFirestoreUseCase failure: ${result.message}")
                    }
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
