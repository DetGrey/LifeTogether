package com.example.lifetogether.domain.model

sealed class SaveProgress {
    data class Loading(val current: Int, val total: Int) : SaveProgress()
    data class Finished(val successCount: Int, var failureCount: Int) : SaveProgress()
    data class Error(val message: String) : SaveProgress()
}
