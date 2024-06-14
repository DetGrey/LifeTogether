package com.example.lifetogether.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.text.TextDisplayLarge

@Composable
fun TopBar(
    leftIcon: Icon,
    text: String,
    rightIcon: Icon,
    subText: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Image(
            painter = painterResource(id = leftIcon.resId),
            contentDescription = leftIcon.description,
        )

        TextDisplayLarge(text = text)

        Image(
            painter = painterResource(id = rightIcon.resId),
            contentDescription = rightIcon.description,
        )
    }

    if (subText != null) {
        Text(
            text = subText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
