package com.example.lifetogether.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.theme.AppTypography
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    icon: Icon,
    title: String,
    titleClickable: (() -> Unit)? = null,
    link: String? = null,
    linkClickable: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp)
            .clip(shape = MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.onBackground),
    ) {
        Row {
            Box(
                modifier = Modifier
                    .padding(LifeTogetherTokens.spacing.xSmall)
                    .aspectRatio(1f)
                    .weight(1f),
            ) {
                Icon(
                    painter = painterResource(id = icon.resId),
                    contentDescription = icon.description,
                    tint = MaterialTheme.colorScheme.background,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(3f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    modifier = Modifier
                        .then(
                            if (titleClickable != null) {
                                Modifier.clickable { titleClickable() }
                            } else {
                                Modifier
                            },
                        ),
                    text = if (titleClickable != null) "$title >" else title,
                    color = if (titleClickable != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.background,
                )

                if (link != null) {
                    Text(
                        modifier = Modifier
                            .padding(top = LifeTogetherTokens.spacing.xSmall)
                            .then(
                                if (linkClickable != null) {
                                    Modifier.clickable { linkClickable() }
                                } else {
                                    Modifier
                                },
                            ),
                        text = "$link >",
                        color = if (linkClickable != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.background,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(LifeTogetherTokens.spacing.xSmall)
                    .aspectRatio(1f)
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
//                Slider(state = SliderState(value = 0f, steps = 1, valueRange = 0f..1f))
            }
        }
    }
}

@Preview
@Composable
fun SettingsItemPreview() {
    LifeTogetherTheme {
        ProvideTextStyle(value = AppTypography.bodyMedium) {
            SettingsItem(
                icon = Icon(R.drawable.ic_profile_picture, "profile icon"),
                title = "Username",
                link = "Edit my profile",
            )
        }
    }
}
