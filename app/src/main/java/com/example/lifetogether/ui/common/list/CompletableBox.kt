package com.example.lifetogether.ui.common.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun CompletableBox(
    isCompleted: Boolean,
    onCompleteToggle: () -> Unit,
    isEnabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    tint: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val background = if (isCompleted) color else Color.Transparent
    Box(
        modifier = Modifier
            .height(30.dp)
            .aspectRatio(1f)
            .clip(shape = CircleShape)
            .border(
                width = 2.dp,
                color = color,
                shape = CircleShape,
            )
            .clickable(enabled = isEnabled) { onCompleteToggle() }
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (isCompleted) {
            Icon(
                painter = painterResource(id = R.drawable.ic_checkmark),
                contentDescription = "checkmark icon",
                tint = tint,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    LifeTogetherTheme {
        Column {
            CompletableBox(
                isCompleted = false,
                onCompleteToggle = {}
            )
            CompletableBox(
                isCompleted = true,
                onCompleteToggle = {}
            )
        }
    }
}