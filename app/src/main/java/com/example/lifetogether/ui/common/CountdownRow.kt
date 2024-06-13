package com.example.lifetogether.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun CountdownRow(
    event: String,
    daysLeft: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextHeadingMedium(text = event)
        TextHeadingMedium(text = "$daysLeft days left")
    }
}

@Preview(showBackground = true)
@Composable
fun CountdownRowPreview() {
    LifeTogetherTheme {
        CountdownRow(
            "Wedding anniversary",
            "20",
        )
    }
}
