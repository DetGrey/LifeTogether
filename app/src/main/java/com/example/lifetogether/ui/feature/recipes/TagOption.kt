package com.example.lifetogether.ui.feature.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun TagOption(
    tag: String,
    selectedTag: String,
    onClick: (String) -> Unit,
) {
    val selected: Boolean = selectedTag == tag
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(shape = RoundedCornerShape(50))
            .background(color = if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (selected) Color.Transparent else Color.Black,
                shape = RoundedCornerShape(50),
            )
            .clickable { onClick(tag) }
            .padding(vertical = 5.dp)
            .padding(horizontal = if (tag.length < 5) 20.dp else 15.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = tag,
            color = if (selected) Color.White else Color.Black,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TagOptionPreview() {
    LifeTogetherTheme {
        Row {
            TagOption(
                tag = "All",
                selectedTag = "All",
                onClick = {},
            )
            TagOption(
                tag = "Dinner",
                selectedTag = "All",
                onClick = {},
            )
            TagOption(
                tag = "Lunch",
                selectedTag = "All",
                onClick = {},
            )
            TagOption(
                tag = "Easy",
                selectedTag = "All",
                onClick = {},
            )
        }
    }
}
