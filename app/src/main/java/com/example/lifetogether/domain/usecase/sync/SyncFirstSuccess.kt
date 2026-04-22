package com.example.lifetogether.domain.usecase.sync

import kotlinx.coroutines.CompletableDeferred

internal fun CompletableDeferred<Result<Unit>>.completeFirstSuccessIfNeeded() {
    if (!isCompleted) {
        complete(Result.success(Unit))
    }
}
