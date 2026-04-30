package com.example.lifetogether.ui.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
fun FlowRowScope.FeatureCard(
    title: String,
    fullWidth: Boolean = false,
    onClick: () -> Unit,
    icon: Icon = Icon(R.drawable.ic_reload, "icon not found"),
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier.weight(1f))
            .fillMaxRowHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .padding(LifeTogetherTokens.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(id = icon.resId),
                contentDescription = icon.description,
                modifier = Modifier.height(50.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )

//        TextHeadingMedium(text = title)
            TextSubHeadingMedium(
                text = title,
                alignCenter = true,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true)
@Composable
fun FeatureCardPreview() {
    LifeTogetherTheme {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 3,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        ) {
            FeatureCard(
                "Grocery list",
                onClick = {},
            )
            FeatureCard(
                "Recipes",
                onClick = {},
            )
            FeatureCard(
                "Third",
                onClick = {},
            )
            FeatureCard(
                "Memory lane",
                true,
                onClick = {},
            )
            FeatureCard(
                "Gallery",
                onClick = {},
            )
            FeatureCard(
                "Note Corner",
                onClick = {},
            )
        }
    }
}
