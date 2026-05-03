package com.example.lifetogether.ui.feature.admin.groceryList.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.ui.common.text.TextBodyLarge
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun ListEditorContainer(
    list: List<String>,
    onDelete: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LifeTogetherTokens.spacing.small),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
    ) {
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primaryContainer)
        for (item in list) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LifeTogetherTokens.sizing.iconMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextBodyLarge(item)
                Icon(
                    painter = painterResource(id = R.drawable.ic_trashcan),
                    contentDescription = "trashcan icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clickable { onDelete(item) },
                )
            }
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primaryContainer)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListEditorContainerPreview() {
    LifeTogetherTheme {
        ListEditorContainer(listOf("🍎 Fruits and vegetables", "🍞 Bakery", "❄️ Frozen food")) { }
    }
}
