package com.example.lifetogether.domain.usecase.observers

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

data class ObserverStartHandle(
    val firstSuccess: Deferred<Result<Unit>>,
    val job: Job,
)
