package com.example.lifetogether.domain.model.sealed

sealed class UploadState {
    data object Idle : UploadState()
    data object Uploading : UploadState()
    data object Success : UploadState()
    data class Failure(val error: String) : UploadState()
}
