package com.example.lifetogether.domain.listener

sealed class ResultListener {
    object Success : ResultListener()
    data class Failure(val message: String) : ResultListener()
}
