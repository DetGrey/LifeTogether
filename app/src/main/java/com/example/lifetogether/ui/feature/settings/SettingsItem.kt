package com.example.lifetogether.ui.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.ui.theme.AppTypography
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun SettingsItem(
    appIcon: AppIcon,
    title: String,
    isTitleClickable: Boolean = false,
    link: String? = null,
    isLinkClickable: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.clickable(
                enabled = onClick != null
            ) {
                onClick?.invoke()
            }
        ) {
            Box(
                modifier = Modifier
                    .padding(LifeTogetherTokens.spacing.xSmall)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = appIcon.resId),
                    contentDescription = appIcon.description,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(3f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (isTitleClickable) "$title >" else title,
                    color = if (isTitleClickable) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimaryContainer,
                )

                if (link != null) {
                    Text(
                        modifier = Modifier.padding(top = LifeTogetherTokens.spacing.xSmall),
                        text = "$link >",
                        color = if (isLinkClickable) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SettingsItemPreview() {
    LifeTogetherTheme {
        ProvideTextStyle(value = AppTypography.bodyMedium) {
            SettingsItem(
                appIcon = AppIcon(R.drawable.ic_profile, "profile icon"),
                title = "Username",
                link = "Edit my profile",
            )
        }
    }
}
