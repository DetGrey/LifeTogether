package com.example.lifetogether.ui.feature.tipTracker.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.common.text.TextHeadingLarge
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun StatsCard(
    title: String,
    total: String,
    average: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LifeTogetherTokens.spacing.small),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LifeTogetherTokens.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextHeadingLarge(text = title)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Total:")
                    TextHeadingMedium(text = total)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Average:")
                    TextHeadingMedium(text = average)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    LifeTogetherTheme {
        StatsCard(
            "title",
            total = "10.0 kr.",
            average = "5.4 kr."
        )
    }
}