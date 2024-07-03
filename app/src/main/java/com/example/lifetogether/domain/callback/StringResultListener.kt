package com.example.lifetogether.domain.callback

sealed class StringResultListener {
    data class Success(val string: String) : StringResultListener()
    data class Failure(val message: String) : StringResultListener()
}
