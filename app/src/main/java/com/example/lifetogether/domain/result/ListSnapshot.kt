package com.example.lifetogether.domain.result

data class ListSnapshot<T>(
    val items: List<T>,
    val isFromCache: Boolean = false,
)
