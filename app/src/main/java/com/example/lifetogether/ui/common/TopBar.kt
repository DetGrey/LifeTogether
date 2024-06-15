package com.example.lifetogether.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.text.TextDisplayLarge
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun TopBar(
    leftIcon: Icon,
    onLeftClick: (() -> Unit)? = null,
    text: String,
    rightIcon: Icon? = null,
    onRightClick: (() -> Unit)? = null,
    subText: String? = null,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .aspectRatio(1f)
                    .clickable {
                        if (onLeftClick != null) {
                            onLeftClick()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = leftIcon.resId),
                    contentDescription = leftIcon.description,
                )
            }

            TextDisplayLarge(text = text)

            Box(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .aspectRatio(1f)
                    .clickable {
                        if (onRightClick != null) {
                            onRightClick()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (rightIcon != null) {
                    Image(
                        painter = painterResource(id = rightIcon.resId),
                        contentDescription = rightIcon.description,
                    )
                } else {
                    // Add a placeholder content (e.g., Spacer) here
                    Spacer(modifier = Modifier.fillMaxSize())
                }
            }
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
}

@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    LifeTogetherTheme {
        TopBar(
            leftIcon = Icon(
                resId = R.drawable.ic_profile_picture,
                description = "",
            ),
            text = "A Life Together",
            rightIcon = Icon(
                resId = R.drawable.ic_settings,
                description = "",
            ),
            subText = "x days together",
        )
    }
}
