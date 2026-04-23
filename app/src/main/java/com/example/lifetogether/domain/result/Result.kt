package com.example.lifetogether.domain.result

/**
 * Functional result type for one-shot data/domain operations.
 */
sealed interface Result<out T, out E : AppError> {
    data class Success<T>(val data: T) : Result<T, Nothing>
    data class Failure(val error: AppError) : Result<Nothing, AppError>
}
