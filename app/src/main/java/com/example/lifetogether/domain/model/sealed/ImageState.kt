package com.example.lifetogether.domain.model.sealed

sealed interface ImageState {
    data object Loading : ImageState

    data object Empty : ImageState

    data class Loaded(
        val bytes: ByteArray,
    ) : ImageState

    data class Error(
        val message: String,
    ) : ImageState
}