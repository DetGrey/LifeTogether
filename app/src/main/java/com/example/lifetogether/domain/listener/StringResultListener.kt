package com.example.lifetogether.domain.listener

sealed class StringResultListener {
    data class Success(val string: String) : StringResultListener()
    data class Failure(val message: String) : StringResultListener()
}
