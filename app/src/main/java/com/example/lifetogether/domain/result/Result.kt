package com.example.lifetogether.domain.result

/**
 * Functional result type for one-shot data/domain operations.
 *
 * Contract for Phase 2:
 * - one-shot commands/refresh operations return [Result]
 * - stream observations are exposed as Flow<T> from local SSOT
 */
sealed interface Result<out T, out E> {
    data class Success<T>(val data: T) : Result<T, Nothing>
    data class Failure<E>(val error: E) : Result<Nothing, E>
}
