package com.example.lifetogether.ui.feature.loading

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.ui.common.text.TextDisplayLarge
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier
            .padding(bottom = 60.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "LifeTogether logo",
                tint = Color.Black,
            )
        }
        TextDisplayLarge("LifeTogether")
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingScreenPreview() {
    LifeTogetherTheme {
        LoadingScreen()
    }
}
