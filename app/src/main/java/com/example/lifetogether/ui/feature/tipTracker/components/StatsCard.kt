package com.example.lifetogether.ui.feature.tipTracker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.common.text.TextHeadingLarge
import com.example.lifetogether.ui.common.text.TextHeadingMedium

@Composable
fun StatsCard(
    title: String,
    total: String,
    average: String,
) {
    Box(
        modifier = Modifier
            .padding(10.dp)
            .background(Color.White, shape = RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .height(120.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
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
