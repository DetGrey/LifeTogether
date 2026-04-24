package com.example.lifetogether.domain.usecase.observers

import android.util.Log
import com.example.lifetogether.data.local.source.UserListLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveUserListsUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val userListLocalDataSource: UserListLocalDataSource,
) {
    private companion object {
        const val TAG = "ObserveUserListsUseCase"
    }

    fun start(
        scope: CoroutineScope,
        uid: String,
        familyId: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            Log.d(TAG, "invoke uid=$uid familyId=$familyId")

            var lastShared: List<UserList> = emptyList()
            var lastPrivate: List<UserList> = emptyList()
            var sharedSynced = false
            var privateSynced = false

            combine(
                firestoreDataSource.familySharedUserListsSnapshotListener(familyId),
                firestoreDataSource.privateUserListsSnapshotListener(familyId, uid),
            ) { s, p -> s to p }.collect { (sharedResult, privateResult) ->
                val shared: List<UserList> = when (sharedResult) {
                    is AppResult.Success -> {
                        sharedSynced = true
                        sharedResult.data.items.also { lastShared = it }
                    }
                    is AppResult.Failure -> {
                        Log.e(TAG, "shared failure: ${sharedResult.error}")
                        lastShared
                    }
                }
                val private: List<UserList> = when (privateResult) {
                    is AppResult.Success -> {
                        privateSynced = true
                        privateResult.data.items.also { lastPrivate = it }
                    }
                    is AppResult.Failure -> {
                        Log.e(TAG, "private failure: ${privateResult.error}")
                        lastPrivate
                    }
                }

                val hadSuccess = sharedResult is AppResult.Success ||
                    privateResult is AppResult.Success
                val anySynced = sharedSynced || privateSynced
                if (!hadSuccess && !anySynced) return@collect

                val merged = (shared + private)
                    .associateBy { it.id ?: "" }
                    .values.filter { !it.id.isNullOrBlank() }
                val fullCoverage = sharedSynced && privateSynced

                runCatching {
                    if (merged.isEmpty()) {
                        if (fullCoverage) userListLocalDataSource.deleteFamilyUserLists(familyId)
                    } else {
                        if (fullCoverage) userListLocalDataSource.updateUserLists(merged.toList())
                        else userListLocalDataSource.upsertUserLists(merged.toList())
                    }
                }.onSuccess {
                    if (anySynced) firstSuccess.completeFirstSuccessIfNeeded()
                }.onFailure { Log.e(TAG, "local update failure: ${it.message}", it) }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
