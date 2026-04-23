package com.example.lifetogether.data.logic

import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result

inline fun <T> appResultOf(block: () -> T): Result<T, AppError> =
    try {
        Result.Success(block())
    } catch (throwable: Throwable) {
        Result.Failure(AppErrors.fromThrowable(throwable))
    }

suspend inline fun <T> appResultOfSuspend(crossinline block: suspend () -> T): Result<T, AppError> =
    try {
        Result.Success(block())
    } catch (throwable: Throwable) {
        Result.Failure(AppErrors.fromThrowable(throwable))
    }
