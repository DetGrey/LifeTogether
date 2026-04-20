package com.example.lifetogether.data.model

import androidx.room.ColumnInfo

data class GalleryMediaIdWithUri(
    val id: String,
    @ColumnInfo(name = "media_uri")
    val mediaUri: String?,
)
