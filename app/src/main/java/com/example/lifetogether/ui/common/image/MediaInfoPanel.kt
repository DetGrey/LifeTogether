package com.example.lifetogether.ui.common.image

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.logic.durationToString
import com.example.lifetogether.domain.logic.toDateTimeString
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.ui.common.text.TextDefault

@Composable
fun MediaInfoPanel(
    media: GalleryMedia,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextDefault(
            text = media.dateCreated?.toDateTimeString() ?: "Unknown date"
        )

        if (media is GalleryVideo) {
            TextDefault(
                text = media.duration?.durationToString() ?: "Unknown duration"
            )
        }

    }
}