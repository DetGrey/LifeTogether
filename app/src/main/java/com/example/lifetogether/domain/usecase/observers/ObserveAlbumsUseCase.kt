package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.source.AlbumLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveAlbumsUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val albumLocalDataSource: AlbumLocalDataSource,
) {
    fun start(
        scope: CoroutineScope,
        familyId: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            println("ObserveAlbumsUseCase invoked")
            firestoreDataSource.albumsSnapshotListener(familyId).collect { result ->
                println("albumsSnapshotListener().collect result: $result")
                when (result) {
                    is AppResult.Success -> {
                        runCatching {
                            if (result.data.items.isEmpty()) {
                                println("albumsSnapshotListener().collect result: is empty")
                                albumLocalDataSource.deleteFamilyAlbums(familyId)
                            } else {
                                albumLocalDataSource.updateAlbums(result.data.items)
                            }
                        }.onSuccess {
                            firstSuccess.completeFirstSuccessIfNeeded()
                        }.onFailure { error ->
                            println("ObserveAlbumsUseCase local update failure: ${error.message}")
                        }
                    }
                    is AppResult.Failure -> {
                        // Keep listener alive; firstSuccess is one-shot and only completes on success.
                        println("ObserveAlbumsUseCase failure: ${result.error}")
                    }
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
