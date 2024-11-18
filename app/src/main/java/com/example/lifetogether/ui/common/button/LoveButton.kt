package com.example.lifetogether.ui.common.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun LoveButton() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(shape = CircleShape)
                .background(color = MaterialTheme.colorScheme.tertiary),
//            .clickable {  } TODO
        ) {
            Image(painter = painterResource(id = R.drawable.ic_heart), contentDescription = "heart icon")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoveButtonPreview() {
    LifeTogetherTheme {
        LoveButton()
    }
}
