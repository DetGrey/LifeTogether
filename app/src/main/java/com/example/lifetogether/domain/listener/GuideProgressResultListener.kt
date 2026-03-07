package com.example.lifetogether.domain.listener

import com.example.lifetogether.domain.model.guides.GuideProgressState

sealed class GuideProgressResultListener {
    data class Success(
        val listItems: List<GuideProgressState>,
        val isFromCache: Boolean = false,
    ) : GuideProgressResultListener()

    data class Failure(
        val message: String,
    ) : GuideProgressResultListener()
}
