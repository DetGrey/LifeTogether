package com.example.lifetogether.domain.listener

import com.example.lifetogether.ui.model.AlbumUiModel

sealed class AlbumUiModelResultListener {
    data class Success(
        val albums: List<AlbumUiModel>
    ): AlbumUiModelResultListener()
    data class Failure(val message: String) : AlbumUiModelResultListener()
}