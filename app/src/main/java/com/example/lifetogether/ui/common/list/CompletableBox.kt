package com.example.lifetogether.ui.common.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R

@Composable
fun CompletableBox(
    isCompleted: Boolean,
    onCompleteToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .aspectRatio(1f)
            .clip(shape = CircleShape)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
            )
            .clickable { onCompleteToggle() }
            .then(
                if (isCompleted) {
                    Modifier.background(color = MaterialTheme.colorScheme.secondary)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isCompleted) {
            Image(
                painter = painterResource(id = R.drawable.ic_checkmark),
                contentDescription = "checkmark icon",
            )
        }
    }
}
