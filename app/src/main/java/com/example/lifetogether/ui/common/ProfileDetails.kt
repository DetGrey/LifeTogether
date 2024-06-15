package com.example.lifetogether.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun ProfileDetails(
    icon: Icon,
    title: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape = RoundedCornerShape(20))
            .background(color = Color.White)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                },
            ),
    ) {
        Row {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.12f)
                    .background(color = MaterialTheme.colorScheme.secondary),
            ) {
                Icon(
                    modifier = Modifier
                        .fillMaxSize(),
                    painter = painterResource(id = icon.resId),
                    contentDescription = icon.description,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .fillMaxHeight()
                    .padding(start = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = title,
                    textAlign = TextAlign.Center,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row {
                    Text(
                        text = value,
                        textAlign = TextAlign.Center,
                    )

                    if (onClick != null) {
                        Spacer(modifier = Modifier.width(5.dp))

                        Text(
                            text = ">",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ProfileDetailsPreview() {
    LifeTogetherTheme {
        ProfileDetails(
            icon = Icon(R.drawable.ic_profile, ""),
            title = "Name",
            value = "Ane",
        )
    }
}
