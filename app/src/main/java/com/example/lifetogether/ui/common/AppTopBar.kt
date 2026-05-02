package com.example.lifetogether.ui.common

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.text.TextDisplayLarge
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    leftIcon: Icon,
    onLeftClick: (() -> Unit)? = null,
    text: String,
    rightIcon: Icon? = null,
    onRightClick: (() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = {
            TextDisplayLarge(
                text = text,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = { onLeftClick?.invoke() },
            ) {
                Icon(
                    painter = painterResource(id = leftIcon.resId),
                    contentDescription = leftIcon.description,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        actions = {
            if (rightIcon != null) {
                IconButton(
                    onClick = { onRightClick?.invoke() },
                ) {
                    Icon(
                        painter = painterResource(id = rightIcon.resId),
                        contentDescription = rightIcon.description,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = Color.Unspecified,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun AppTopBarPreview() {
    LifeTogetherTheme {
        AppTopBar(
            leftIcon = Icon(
                resId = R.drawable.ic_profile_picture,
                description = "",
            ),
            text = "A Life Together",
            rightIcon = Icon(
                resId = R.drawable.ic_settings,
                description = "",
            ),
        )
    }
}
