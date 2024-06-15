package com.example.lifetogether.domain.callback

sealed class ResultListener {
    data object Success : ResultListener()
    data class Failure(val message: String) : ResultListener()
}
