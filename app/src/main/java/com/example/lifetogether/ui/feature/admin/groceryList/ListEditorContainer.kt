package com.example.lifetogether.ui.feature.admin.groceryList

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun ListEditorContainer(
    list: List<String>,
    onDelete: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
            for (item in list) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(25.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextDefault(item)
                    Image(
                        painter = painterResource(id = R.drawable.ic_trashcan_black),
                        contentDescription = "trashcan icon",
                        modifier = Modifier.clickable { onDelete(item) },
                    )
                }
                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
            }
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
