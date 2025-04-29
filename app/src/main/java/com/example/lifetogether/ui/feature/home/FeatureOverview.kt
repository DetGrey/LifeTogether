package com.example.lifetogether.ui.feature.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun RowScope.FeatureOverview(
    title: String,
    fullWidth: Boolean = false,
    onClick: () -> Unit,
    icon: Icon = Icon(R.drawable.ic_reload, "icon not found"),
) {
    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(20))
            .background(MaterialTheme.colorScheme.onBackground)
            .padding(horizontal = 10.dp, vertical = 20.dp)
            .clickable { onClick() }
            .then(
                if (fullWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.weight(0.5f)
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = icon.resId),
            contentDescription = icon.description,
            modifier = Modifier.height(60.dp),
        )

        TextHeadingMedium(text = title)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
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
                onClick = {},
            )
            FeatureOverview(
                "Recipes",
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
