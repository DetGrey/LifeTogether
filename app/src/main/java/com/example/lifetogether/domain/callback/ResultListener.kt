package com.example.lifetogether.domain.callback

sealed class ResultListener {
    object Success : ResultListener()
    data class Failure(val message: String) : ResultListener()
}
