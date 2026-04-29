package com.example.lifetogether.ui.common.tagOptionRow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun TagOption(
    tag: String,
    selectedTag: String,
    onClick: ((String) -> Unit)? = null,
) {
    val selected: Boolean = selectedTag == tag
    FilterChip(
        modifier = Modifier.height(30.dp),
        selected = selected,
        enabled = onClick != null,
        onClick = { onClick?.invoke(tag) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onBackground,
            selectedContainerColor = MaterialTheme.colorScheme.secondary,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.onBackground,
            selectedBorderColor = Color.Transparent,
            enabled = onClick != null,
            selected = selected,
        ),
        label = {
            Text(
                text = tag,
                color = if (selected) MaterialTheme.colorScheme.onSecondary
                    else MaterialTheme.colorScheme.onBackground,
            )
        },
        shape = CircleShape,
    )
}

@Preview(showBackground = true)
@Composable
fun ListEntryDetailsScreenPreview() {
    LifeTogetherTheme {
        Column {
            TagOption(
                tag = "Mon",
                selectedTag = "",
                onClick = {},
            )
            TagOption(
                tag = "Mon",
                selectedTag = "Mon",
                onClick = {},
            )
        }
    }
}
