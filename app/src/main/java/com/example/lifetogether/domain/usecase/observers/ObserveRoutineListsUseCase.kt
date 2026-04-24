package com.example.lifetogether.domain.usecase.observers

import android.util.Log
import com.example.lifetogether.data.local.source.UserListLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveRoutineListsUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val storageDataSource: StorageDataSource,
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
                    is AppResult.Success -> {
                        Log.d(TAG, "snapshot count=${result.data.items.size}")
                        runCatching {
                            if (result.data.items.isEmpty()) {
                                userListLocalDataSource.deleteFamilyRoutineListEntries(familyId)
                            } else {
                                val existingIdsWithImages = userListLocalDataSource.getRoutineEntryIdsWithImages(familyId)

                                val byteArrays: MutableMap<String, ByteArray> = mutableMapOf()
                                for (entry in result.data.items) {
                                    if (entry.id != null && existingIdsWithImages.contains(entry.id)) {
                                        Log.d(TAG, "Skipping download for ${entry.itemName} — image already cached")
                                        continue
                                    }
                                    val byteArrayResult: AppResult<ByteArray, String>? =
                                        entry.imageUrl?.let { url ->
                                            storageDataSource.fetchImageByteArray(url)
                                        }
                                    if (byteArrayResult is AppResult.Success) {
                                        entry.id?.let { byteArrays[it] = byteArrayResult.data }
                                    }
                                }

                                userListLocalDataSource.updateRoutineListEntries(result.data.items, byteArrays)
                            }
                        }.onSuccess {
                            firstSuccess.completeFirstSuccessIfNeeded()
                        }.onFailure { Log.e(TAG, "local update failure: ${it.message}", it) }
                    }
                    is AppResult.Failure -> {
                        Log.e(TAG, "listener failure: ${result.error}")
                    }
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
