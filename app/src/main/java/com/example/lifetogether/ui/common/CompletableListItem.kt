package com.example.lifetogether.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifetogether.ui.common.list.CompletableBox

@Composable
fun CompletableListItem(
    text: String,
    isCompleted: Boolean,
    onCompleteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    ListItem(
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            CompletableBox(
                isCompleted = isCompleted,
                onCompleteToggle = onCompleteToggle,
            )
        },
        headlineContent = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        ),
    )
}
