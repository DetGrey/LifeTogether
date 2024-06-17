package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.bodyFontFamily
import java.util.Date

@Composable
fun ItemCategoryList(
    category: Category,
    itemList: List<GroceryItem>,
    expanded: Boolean,
    onClick: () -> Unit,
    onCompleteToggle: (GroceryItem) -> Unit,
) {
    Column {
        Column(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .clickable { onClick() },
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(25.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row {
                    Text(
                        text = category.emoji,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = bodyFontFamily,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Image(
                    painter = painterResource(id = if (expanded) R.drawable.ic_expanded else R.drawable.ic_expand),
                    contentDescription = "expand or expanded icon",
                )
            }
            Divider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                itemList.forEach { item ->
                    ListItem(
                        item = item,
                        onCompleteToggle = { onCompleteToggle(item) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun ItemCategoryListPreview() {
    LifeTogetherTheme {
        ItemCategoryList(
            category = Category(
                "🍎",
                "Fruits and vegetables",
            ),
            itemList = groceryList,
            true,
            onClick = {},
            onCompleteToggle = {},
        )
    }
}

val groceryList = listOf(
    GroceryItem(
        uid = "dsuaihfao",
        category = Category(
            "🍎",
            "Fruits and vegetables",
        ),
        itemName = "Bananas",
        lastUpdated = Date(System.currentTimeMillis()),
        completed = false,
    ),
    GroceryItem(
        uid = "dsuaihfao",
        category = Category(
            "🍎",
            "Fruits and vegetables",
        ),
        itemName = "Potatoes",
        lastUpdated = Date(System.currentTimeMillis()),
        completed = true,
    ),
)
