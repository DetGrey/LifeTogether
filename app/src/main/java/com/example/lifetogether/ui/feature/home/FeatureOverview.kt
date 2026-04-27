package com.example.lifetogether.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowScope.FeatureOverview(
    title: String,
    fullWidth: Boolean = false,
    onClick: () -> Unit,
    icon: Icon = Icon(R.drawable.ic_reload, "icon not found"),
) {
    Column(
        modifier = Modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier.weight(1f))
            .fillMaxRowHeight()
            .background(
                MaterialTheme.colorScheme.onBackground,
                MaterialTheme.shapes.large
            )
            .padding(LifeTogetherTokens.spacing.small)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(id = icon.resId),
            contentDescription = icon.description,
            modifier = Modifier.height(50.dp),
        )

//        TextHeadingMedium(text = title)
        TextSubHeadingMedium(text = title, alignCenter = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true)
@Composable
fun FeatureOverviewPreview() {
    LifeTogetherTheme {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 3,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        ) {
            FeatureOverview(
                "Grocery list",
                onClick = {},
            )
            FeatureOverview(
                "Recipes",
                onClick = {},
            )
            FeatureOverview(
                "Third",
                onClick = {},
            )
            FeatureOverview(
                "Memory lane",
                true,
                onClick = {},
            )
            FeatureOverview(
                "Gallery",
                onClick = {},
            )
            FeatureOverview(
                "Note Corner",
                onClick = {},
            )
        }
    }
}
