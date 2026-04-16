package com.example.lifetogether.domain.result

sealed interface AppError {
    val message: String

    data class Network(override val message: String) : AppError
    data class Authentication(override val message: String) : AppError
    data class PermissionDenied(override val message: String) : AppError
    data class NotFound(override val message: String) : AppError
    data class Conflict(override val message: String) : AppError
    data class Validation(override val message: String) : AppError
    data class Storage(override val message: String) : AppError
    data class Serialization(override val message: String) : AppError
    data class Unknown(override val message: String) : AppError
}
