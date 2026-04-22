package com.example.lifetogether.domain.usecase.sync

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

data class SyncStartHandle(
    val firstSuccess: Deferred<Result<Unit>>,
    val job: Job,
)
