package com.example.lifetogether.domain.usecase.observers

import android.util.Log
import com.example.lifetogether.data.local.source.UserListLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.repository.StorageRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveRoutineListsUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val storageRepository: StorageRepository,
    private val userListLocalDataSource: UserListLocalDataSource,
) {
    private companion object {
        const val TAG = "ObserveRoutineListsUseCase"
    }

    fun start(
        scope: CoroutineScope,
        familyId: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            Log.d(TAG, "invoke familyId=$familyId")
            firestoreDataSource.familyRoutineListEntriesSnapshotListener(familyId).collect { result ->
                when (result) {
                    is ListItemsResultListener.Success -> {
                        Log.d(TAG, "snapshot count=${result.listItems.size}")
                        runCatching {
                            if (result.listItems.isEmpty()) {
                                userListLocalDataSource.deleteFamilyRoutineListEntries(familyId)
                            } else {
                                val existingIdsWithImages = userListLocalDataSource.getRoutineEntryIdsWithImages(familyId)

                                val byteArrays: MutableMap<String, ByteArray> = mutableMapOf()
                                for (entry in result.listItems) {
                                    if (entry.id != null && existingIdsWithImages.contains(entry.id)) {
                                        Log.d(TAG, "Skipping download for ${entry.itemName} — image already cached")
                                        continue
                                    }
                                    val byteArrayResult: ByteArrayResultListener? =
                                        entry.imageUrl?.let { url ->
                                            storageRepository.fetchImageByteArray(url)
                                        }
                                    if (byteArrayResult is ByteArrayResultListener.Success) {
                                        entry.id?.let { byteArrays[it] = byteArrayResult.byteArray }
                                    }
                                }

                                userListLocalDataSource.updateRoutineListEntries(result.listItems, byteArrays)
                            }
                        }.onSuccess {
                            firstSuccess.completeFirstSuccessIfNeeded()
                        }.onFailure { Log.e(TAG, "local update failure: ${it.message}", it) }
                    }
                    is ListItemsResultListener.Failure -> {
                        Log.e(TAG, "listener failure: ${result.message}")
                    }
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
