package com.example.lifetogether.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun RowScope.FeatureOverview(
    title: String,
    itemCount: Int,
    itemType: String,
    fullWidth: Boolean = false,
) {
    var customModifier = Modifier
        .weight(0.5f)
        .clip(shape = RoundedCornerShape(20))
        .background(MaterialTheme.colorScheme.onBackground)
        .padding(horizontal = 10.dp, vertical = 20.dp)

    if (fullWidth) {
        customModifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(20))
            .background(MaterialTheme.colorScheme.onBackground)
            .padding(horizontal = 10.dp, vertical = 20.dp)
    }

    var itemTypeText = itemType
    if (itemCount > 1) {
        itemTypeText = "${itemType}s"
    }

    Column(
        modifier = customModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image( // TODO add icon and description
            painter = painterResource(id = R.drawable.ic_gallery),
            contentDescription = "",
            modifier = Modifier.height(60.dp),
        )

        TextHeadingMedium(text = title)

        Text(
            text = "$itemCount $itemTypeText",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true)
@Composable
fun FeatureOverviewPreview() {
    LifeTogetherTheme {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FeatureOverview(
                "Grocery list",
                10,
                "Recipe",
            )
            FeatureOverview(
                "Recipes",
                1,
                "Recipe",
            )
            FeatureOverview(
                "Memory lane",
                4,
                "Recipe",
                true,
            )
            FeatureOverview(
                "Gallery",
                10,
                "Recipe",
            )
            FeatureOverview(
                "Note Corner",
                43,
                "Recipe",
            )
        }
    }
}
