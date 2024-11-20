package com.example.lifetogether.ui.common.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun AddButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(shape = CircleShape)
            .background(color = MaterialTheme.colorScheme.tertiary)
            .clickable { onClick() },
    ) {
        Image(painter = painterResource(id = R.drawable.ic_plus), contentDescription = "plus icon")
    }
}

@Preview(showBackground = true)
@Composable
fun AddButtonPreview() {
    LifeTogetherTheme {
        AddButton(onClick = {})
    }
}
