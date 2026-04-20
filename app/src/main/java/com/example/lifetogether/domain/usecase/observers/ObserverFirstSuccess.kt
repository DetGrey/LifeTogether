package com.example.lifetogether.domain.usecase.observers

import kotlinx.coroutines.CompletableDeferred

internal fun CompletableDeferred<Result<Unit>>.completeFirstSuccessIfNeeded() {
    if (!isCompleted) {
        complete(Result.success(Unit))
    }
}
