package com.example.lifetogether.domain.result

fun AppError.toUserMessage(): String {
    return when (this) {
        is AppError.Network -> "Network error. Please try again."
        is AppError.Authentication -> "Authentication failed. Please check your credentials."
        is AppError.PermissionDenied -> "You don't have permission to do that."
        is AppError.NotFound -> "The requested item was not found."
        is AppError.Conflict -> "Could not complete the action due to a conflict."
        is AppError.Validation -> "Please check your input and try again."
        is AppError.Storage -> "Storage error. Please try again."
        is AppError.Serialization -> "Data error. Please try again."
        is AppError.Unknown -> "Something went wrong. Please try again."
    }
}
