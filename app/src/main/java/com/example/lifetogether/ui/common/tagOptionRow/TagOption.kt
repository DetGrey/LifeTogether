package com.example.lifetogether.ui.common.tagOptionRow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun TagOption(
    tag: String,
    selectedTag: String,
    onClick: ((String) -> Unit)? = null,
) {
    val selected: Boolean = selectedTag == tag
    Box(
        modifier = Modifier
            .height(30.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .border(
                width = 2.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.onBackground,
                shape = RoundedCornerShape(50),
            )
            .clickable(
                enabled = onClick != null,
            ) {
                if (onClick != null) {
                    onClick(tag)
                }
            }
            .padding(top = 2.dp, bottom = LifeTogetherTokens.spacing.xSmall)
            .padding(horizontal = if (tag.length < 5) LifeTogetherTokens.spacing.large else LifeTogetherTokens.spacing.medium),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = tag,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ListEntryDetailsScreenPreview() {
    LifeTogetherTheme {
        Column {
            TagOption(
                tag = "Mon",
                selectedTag = ""
            )
            TagOption(
                tag = "Tag",
                selectedTag = "Tag"
            )
        }
    }
}
