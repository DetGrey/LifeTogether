package com.example.lifetogether.ui.common

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    leftAppIcon: AppIcon? = null,
    onLeftClick: (() -> Unit)? = null,
    text: String,
    titleContent: (@Composable () -> Unit)? = null,
    rightAppIcon: AppIcon? = null,
    onRightClick: (() -> Unit)? = null,
    rightText: String? = null,
    onRightTextClick: (() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = {
            if (titleContent != null) {
                titleContent()
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            if (leftAppIcon != null) {
                IconButton(
                    onClick = { onLeftClick?.invoke() },
                ) {
                    Icon(
                        painter = painterResource(id = leftAppIcon.resId),
                        contentDescription = leftAppIcon.description,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
        actions = {
            if (rightText != null) {
                TextButton(onClick = { onRightTextClick?.invoke() }) {
                    Text(
                        text = rightText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else if (rightAppIcon != null) {
                IconButton(
                    onClick = { onRightClick?.invoke() },
                ) {
                    val tint = if (rightAppIcon.resId == R.drawable.ic_delete) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
                    Icon(
                        painter = painterResource(id = rightAppIcon.resId),
                        contentDescription = rightAppIcon.description,
                        tint = tint,
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
private fun AppTopBarPreview() {
    LifeTogetherTheme {
        AppTopBar(
            leftAppIcon = AppIcon(
                resId = R.drawable.ic_profile_picture,
                description = "",
            ),
            text = "A Life Together",
            rightAppIcon = AppIcon(
                resId = R.drawable.ic_settings,
                description = "",
            ),
        )
    }
}
